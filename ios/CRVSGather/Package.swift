// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "CRVSGather",
    platforms: [
        .iOS(.v16),
        .macOS(.v13)
    ],
    products: [
        .library(
            name: "CRVSGather",
            targets: ["CRVSGather"]
        ),
    ],
    dependencies: [],
    targets: [
        .target(
            name: "CRVSGather",
            dependencies: [],
            path: "."
        ),
    ]
)
