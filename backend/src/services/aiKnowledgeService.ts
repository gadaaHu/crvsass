import { db } from '../database/db.js';
import fs from 'node:fs';
import path from 'node:path';

// Dynamic imports for document parsers
let pdfParse: any = null;
let mammoth: any = null;

async function loadParsers() {
  if (!pdfParse) {
    try {
      const mod = await import('pdf-parse');
      pdfParse = mod.default || mod;
    } catch (e) {
      console.warn('pdf-parse not available:', e);
    }
  }
  if (!mammoth) {
    try {
      const mod = await import('mammoth');
      mammoth = mod.default || mod;
    } catch (e) {
      console.warn('mammoth not available:', e);
    }
  }
}

export interface TrainingQuestionContext {
  id: number;
  questionNumber: number;
  sectionTitleAm: string;
  sectionTitleEn: string;
  promptAm: string;
  promptEn: string;
  rubricAm: string | null;
  rubricEn: string | null;
  correctAnswers: string[];
}

export interface KnowledgeDoc {
  id: number;
  filename: string;
  fileType: string;
  title: string;
  content: string;
  summary: string | null;
  charCount: number;
  uploadedAt: string;
}

export class AiKnowledgeService {
  /**
   * Extract text from an uploaded file buffer based on extension
   */
  static async extractTextFromFile(buffer: Buffer, originalFilename: string): Promise<{ text: string; fileType: string }> {
    await loadParsers();
    const ext = path.extname(originalFilename).toLowerCase();

    if (ext === '.pdf') {
      if (pdfParse) {
        try {
          const data = await pdfParse(buffer);
          if (data && data.text && data.text.trim().length > 0) {
            return { text: data.text, fileType: 'pdf' };
          }
        } catch (e) {
          console.warn('pdfParse parsing failed, using fallback:', e);
        }
      }
      // Fallback text extraction for PDF
      const raw = buffer.toString('latin1');
      const textMatches = raw.match(/\(([^)]+)\)\s*Tj/g) || raw.match(/\[([^\]]+)\]\s*TJ/g);
      if (textMatches && textMatches.length > 0) {
        const extracted = textMatches.map(m => m.replace(/^[\(\[]|[\)\]]\s*TJ?$/gi, '')).join(' ');
        if (extracted.trim().length > 20) {
          return { text: extracted.trim(), fileType: 'pdf' };
        }
      }
      const plainFallback = buffer.toString('utf-8').replace(/[^\x20-\x7E\u1200-\u137F\n\r\t]/g, ' ').replace(/\s{2,}/g, ' ');
      if (plainFallback.trim().length > 30) {
        return { text: plainFallback.trim(), fileType: 'pdf' };
      }
      return { text: `PDF Document: ${originalFilename}`, fileType: 'pdf' };
    } else if (ext === '.docx' || ext === '.doc') {
      if (mammoth) {
        const result = await mammoth.extractRawText({ buffer });
        return { text: result.value || '', fileType: 'docx' };
      }
      throw new Error('Word docx parser is not available');
    } else if (ext === '.txt' || ext === '.md' || ext === '.json' || ext === '.csv') {
      const text = buffer.toString('utf-8');
      return { text, fileType: ext.replace('.', '') };
    } else {
      throw new Error(`Unsupported file type: ${ext}. Please upload .pdf, .docx, or .txt`);
    }
  }

  /**
   * Save an extracted document into the knowledge base
   */
  static saveDocument(filename: string, fileType: string, title: string, content: string): KnowledgeDoc {
    const summary = content.trim().substring(0, 300).replace(/\s+/g, ' ') + (content.length > 300 ? '...' : '');
    const charCount = content.length;

    const stmt = db.prepare(`
      INSERT INTO knowledge_documents (filename, file_type, title, content, summary, char_count)
      VALUES (?, ?, ?, ?, ?, ?)
    `);
    const res = stmt.run(filename, fileType, title, content, summary, charCount);
    const id = Number(res.lastInsertRowid);

    return {
      id,
      filename,
      fileType,
      title,
      content,
      summary,
      charCount,
      uploadedAt: new Date().toISOString()
    };
  }

  /**
   * List all uploaded knowledge documents
   */
  static getAllDocuments(): KnowledgeDoc[] {
    try {
      const stmt = db.prepare(`
        SELECT id, filename, file_type as fileType, title, content, summary, char_count as charCount, uploaded_at as uploadedAt
        FROM knowledge_documents
        ORDER BY id DESC
      `);
      return stmt.all() as unknown as KnowledgeDoc[];
    } catch {
      return [];
    }
  }

  /**
   * Delete a document by ID
   */
  static deleteDocument(id: number): boolean {
    const stmt = db.prepare(`DELETE FROM knowledge_documents WHERE id = ?`);
    const res = stmt.run(id);
    return res.changes > 0;
  }

  /**
   * Load all official training questions, correct options, and rubrics from SQLite
   */
  static getTrainingQuestions(): TrainingQuestionContext[] {
    try {
      const stmt = db.prepare(`
        SELECT 
          q.id,
          q.question_number as questionNumber,
          s.title_am as sectionTitleAm,
          s.title_en as sectionTitleEn,
          q.prompt_am as promptAm,
          q.prompt_en as promptEn,
          q.rubric_am as rubricAm,
          q.rubric_en as rubricEn,
          (
            SELECT GROUP_CONCAT(o.label_am, ' | ')
            FROM question_options o
            WHERE o.question_id = q.id AND o.is_correct = 1
          ) as correctAm,
          (
            SELECT GROUP_CONCAT(o.label_en, ' | ')
            FROM question_options o
            WHERE o.question_id = q.id AND o.is_correct = 1
          ) as correctEn
        FROM questions q
        JOIN sections s ON q.section_id = s.id
        ORDER BY q.question_number ASC
      `);

      const rows = stmt.all() as any[];
      return rows.map(r => ({
        id: r.id,
        questionNumber: r.questionNumber,
        sectionTitleAm: r.sectionTitleAm,
        sectionTitleEn: r.sectionTitleEn,
        promptAm: r.promptAm,
        promptEn: r.promptEn,
        rubricAm: r.rubricAm,
        rubricEn: r.rubricEn,
        correctAnswers: [r.correctAm, r.correctEn].filter(Boolean)
      }));
    } catch (e) {
      console.error('Failed to load training questions:', e);
      return [];
    }
  }

  /**
   * Semantic and keyword match search over questions and uploaded documents
   */
  static retrieveContext(query: string, maxQuestions = 3, maxDocChunks = 3): {
    matchedQuestions: TrainingQuestionContext[];
    matchedDocSnippets: { title: string; filename: string; snippet: string }[];
  } {
    // Amharic and common stopwords to dampen
    const stopwords = new Set([
      'ስለ', 'ነው', 'ምን', 'እና', 'ውስጥ', 'ጋር', 'ማን', 'እንዴት', 'ላይ', 'ወይም', 'ወደ',
      'the', 'is', 'what', 'how', 'for', 'and', 'with', 'about', 'from', 'which', 'who'
    ]);

    const tokens = query
      .toLowerCase()
      .replace(/[?!.,;:"'()\[\]{}–—\-፡።፣፤፥፦፧፨/\\@#$%^&*+=<>_~`]/g, ' ')
      .split(/\s+/)
      .filter(t => /[\u1200-\u137F]/.test(t) ? t.length >= 2 : t.length > 2);

    // 1. Search Questions with weighted field relevance
    const allQuestions = this.getTrainingQuestions();
    const scoredQuestions = allQuestions.map(q => {
      const promptText = `${q.promptAm} ${q.promptEn}`.toLowerCase();
      const rubricText = `${q.rubricAm || ''} ${q.rubricEn || ''}`.toLowerCase();
      const answersText = q.correctAnswers.join(' ').toLowerCase();
      const sectionText = `${q.sectionTitleAm} ${q.sectionTitleEn}`.toLowerCase();

      let score = 0;
      for (const t of tokens) {
        const isStop = stopwords.has(t);
        const weight = isStop ? 0.5 : 1.0;

        if (promptText.includes(t)) {
          score += 6 * weight;
        }
        if (answersText.includes(t)) {
          score += 4 * weight;
        }
        if (rubricText.includes(t)) {
          score += 3 * weight;
        }
        if (sectionText.includes(t)) {
          // Section titles are repeated across many questions, give slight boost
          score += 0.8 * weight;
        }
      }
      return { question: q, score };
    })
    .filter(item => item.score > 2)
    .sort((a, b) => b.score - a.score)
    .slice(0, maxQuestions)
    .map(item => item.question);

    // 2. Search Uploaded Documents
    const docs = this.getAllDocuments();
    const matchedDocSnippets: { title: string; filename: string; snippet: string }[] = [];

    for (const doc of docs) {
      // Split into paragraphs / sections
      const paragraphs = doc.content
        .split(/\n\s*\n|\r\n\s*\r\n/)
        .map(p => p.trim())
        .filter(p => p.length > 25);

      const scoredParagraphs = paragraphs.map(p => {
        const lower = p.toLowerCase();
        let score = 0;
        for (const t of tokens) {
          if (lower.includes(t)) {
            const isStop = stopwords.has(t);
            score += isStop ? 0.5 : 2.0;
          }
        }
        return { snippet: p, score };
      })
      .filter(sp => sp.score > 1.5)
      .sort((a, b) => b.score - a.score);

      for (const sp of scoredParagraphs.slice(0, 2)) {
        const snippetText = sp.snippet.length > 500
          ? sp.snippet.substring(0, 500) + '...'
          : sp.snippet;
        matchedDocSnippets.push({
          title: doc.title,
          filename: doc.filename,
          snippet: snippetText
        });
        if (matchedDocSnippets.length >= maxDocChunks) break;
      }
      if (matchedDocSnippets.length >= maxDocChunks) break;
    }

    return {
      matchedQuestions: scoredQuestions,
      matchedDocSnippets
    };
  }

  /**
   * Synthesize a smart training answer using grounded training questions + uploaded docs
   */
  static synthesizeResponse(query: string, language: 'am' | 'en'): string {
    const isAm = language === 'am' || /[\u1200-\u137F]/.test(query);
    const { matchedQuestions, matchedDocSnippets } = this.retrieveContext(query);

    let answer = '';

    // If we have direct matching training questions from the curriculum
    if (matchedQuestions.length > 0) {
      const primaryQ = matchedQuestions[0];
      const rubricText = isAm 
        ? (primaryQ.rubricAm || primaryQ.promptAm)
        : (primaryQ.rubricEn || primaryQ.promptEn);

      answer += isAm
        ? `📖 **ከ CRVS ስልጠና ማዕቀፍ (ጥያቄ #${primaryQ.questionNumber} - ${primaryQ.sectionTitleAm})**\n\n`
        : `📖 **From CRVS Curriculum (Question #${primaryQ.questionNumber} - ${primaryQ.sectionTitleEn})**\n\n`;

      answer += isAm
        ? `**ጥያቄ፡** ${primaryQ.promptAm}\n\n` +
          (primaryQ.correctAnswers.length > 0 ? `**ትክክለኛ መልስ፡** ${primaryQ.correctAnswers.join(' / ')}\n\n` : '') +
          `**የስልጠና ማብራሪያ፡**\n${rubricText}\n\n`
        : `**Question:** ${primaryQ.promptEn}\n\n` +
          (primaryQ.correctAnswers.length > 0 ? `**Correct Answer:** ${primaryQ.correctAnswers.join(' / ')}\n\n` : '') +
          `**Training Rubric & Guideline:**\n${rubricText}\n\n`;
    }

    // If we have matching uploaded documents
    if (matchedDocSnippets.length > 0) {
      answer += isAm
        ? `📄 **ከተጫኑ መመሪያዎች እና ሰነዶች (Knowledge Base):**\n\n`
        : `📄 **From Uploaded Training Documents & Manuals:**\n\n`;

      matchedDocSnippets.forEach(doc => {
        answer += `• **[${doc.title} (${doc.filename})]**:\n"${doc.snippet}"\n\n`;
      });
    }

    // If neither questions nor uploaded documents had a match, fallback to foundational proclamation rules
    if (!answer) {
      const q = query.toLowerCase();
      if (q.includes('1370') || q.includes('አዋጅ') || q.includes('proclamation') || q.includes('ሕግ') || q.includes('ህግ') || q.includes('law')) {
        answer = isAm
          ? `📜 **የወሳኝ ኩነቶች ምዝገባ አዋጅ ቁጥር 1370/2024 (2016 ዓ.ም)**\n\nይህ አዋጅ በኢትዮጵያ የወሳኝ ኩነቶች ምዝገባ ሥርዓትን በዘመናዊ ዲጂታል ቴክኖሎጂ ለማደራጀት የወጣ አዲስ አዋጅ ነው።\n\n**ዋና ዋና ነጥቦች፡**\n1. **የኩነቶች ዓይነቶች፡** ልደት፣ ሞት፣ ጋብቻ፣ ፍቺ፣ ጉዲፈቻ እና አባትነትን ማወቅ።\n2. **የምዝገባ ግዴታ፡** እያንዳንዱ ዜጋ ወይም ነዋሪ ኩነቶች ሲከሰቱ በተቀመጠው የጊዜ ገደብ የማስመዝገብ ግዴታ አለበት።\n3. **ዲጂታል ምዝገባ፡** የወረቀት ሰነዶችን በማስቀረት በማዕከላዊ ዳታቤዝ የተደገፈ ዲጂታል የምስክር ወረቀት በ QR ኮድ ይሰጣል።`
          : `📜 **CRVS Proclamation No. 1370/2024**\n\nGoverns modernized civil registration in Ethiopia through central registers and cryptographic QR verification.`;
      } else if (q.includes('ልደት') || q.includes('birth') || q.includes('ሕፃን') || q.includes('ህፃን')) {
        answer = isAm
          ? `👶 **የልደት ምዝገባ አሰራር እና መመሪያዎች**\n\n• **መደበኛ ምዝገባ፡** ህፃኑ በተወለደ በ **90 ቀናት** ውስጥ ያለ ምንም ቅጣት በነፃ ይመዘገባል።\n• **የዘገየ ምዝገባ፡** ከ 90 ቀናት በኋላ እስከ 1 ዓመት በቅጣት ይመዘገባል።\n\n**አስፈላጊ ሰነዶች፡**\n1. ከጤና ተቋም የልደት ማስታወቂያ ሰነድ (Birth Notification)።\n2. የወላጆች የታደሰ መታወቂያ ወይም ፓስፖርት።\n3. የጋብቻ ምስክር ወረቀት (ካለ)።`
          : `👶 **Birth Registration Guidelines**\n\n• **Standard Deadline:** Within **90 days** from birth without penalty.\n• **Late Registration:** After 90 days up to 1 year.\n• **Required:** Official health institution notification and parents' valid IDs.`;
      } else if (q.includes('ሞት') || q.includes('death')) {
        answer = isAm
          ? `⚰️ **የሞት ምዝገባ አሰራር እና መመሪያዎች**\n\n• **የጊዜ ገደብ፡** ሞት ከተከሰተበት ቀን ጀምሮ በ **30 ቀናት** ውስጥ መመዝገብ አለበት።\n• **አስፈላጊ ሰነዶች፡** ከጤና ተቋም የሞት መንስኤ ማረጋገጫ (MCCD) እና የሟች መታወቂያ።`
          : `⚰️ **Death Registration Guidelines**\n\n• **Deadline:** Within **30 days** of death.\n• **Required:** Medical Certificate of Cause of Death (MCCD) and deceased's ID.`;
      } else if (q.includes('ጋብቻ') || q.includes('marriage')) {
        answer = isAm
          ? `💍 **የጋብቻ ምዝገባ አሰራር**\n\n• **የጊዜ ገደብ፡** ጋብቻ ከተፈጸመበት ቀን ጀምሮ በ **30 ቀናት** ውስጥ።\n• **ቅድመ-ሁኔታ፡** ሁለቱም ተጋቢዎች ቢያንስ 18 ዓመት የሞላቸው እና 2 ምስክሮች መገኘት አለባቸው።`
          : `💍 **Marriage Registration Guidelines**\n\n• **Deadline:** Within **30 days** of marriage.\n• **Criteria:** Both spouses present, minimum age 18, and at least 2 witnesses.`;
      } else {
        answer = isAm
          ? `🤖 **የ CRVS ስልጠና AI ረዳት**\n\nስለ ስልጠናው ስርዓተ-ትምህርት፣ ጥያቄዎች እና ስለ አዋጅ ቁጥር 1370/2024 ዝርዝር ጥያቄዎችን መጠየቅ ይችላሉ።\n\nለምሳሌ፡\n• *"የልደት ምዝገባ ጊዜ ገደብ ስንት ነው?"*\n• *"ሞት በስንት ቀናት ውስጥ መመዝገብ አለበት?"*\n• *"የዲጂታል ሲስተሙ የ QR ኮድ ፋይዳ ምንድን ነው?"*`
          : `🤖 **CRVS Training AI Tutor**\n\nAsk any question related to curriculum questions, Proclamation 1370/2024, or civil registration workflows!`;
      }
    }

    return answer;
  }
}
