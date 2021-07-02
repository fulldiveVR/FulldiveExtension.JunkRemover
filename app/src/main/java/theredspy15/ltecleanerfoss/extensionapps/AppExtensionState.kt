package theredspy15.ltecleanerfoss.extensionapps

sealed class AppExtensionState(val id: String) {
    object Start: AppExtensionState("start")
    object Stop: AppExtensionState("stop")
    object Failure: AppExtensionState("failure")
}

sealed class WorkType(val id: String) {
    object Analyze: WorkType("analyze")
    object Clean: WorkType("clean")
    object Open: WorkType("open")
    object GetPermissionsRequired : WorkType("get_permissions_required")
    object GetStatus : WorkType("get_status")
}