snapshot.isFastR <-
function () {
    length(grep("FastR", R.Version()$version.string)) != 0
}
