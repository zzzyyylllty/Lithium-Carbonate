
rootProject.name = "LithiumCarbon"
include(":project:common")
include(":project:common-files")
include(":plugin")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.version.toml"))
        }
    }
}