package theredspy15.ltecleanerfoss.extensionapps

sealed class AppExtensionState(val id: String) {
    object START: AppExtensionState("START")
    object STOP: AppExtensionState("STOP")
    object FAILURE: AppExtensionState("FAILURE")
}

sealed class WorkType(val id: String) {
    object ANALYZE: WorkType("ANALYZE")
    object CLEAN: WorkType("CLEAN")
    object OPEN: WorkType("OPEN")
}