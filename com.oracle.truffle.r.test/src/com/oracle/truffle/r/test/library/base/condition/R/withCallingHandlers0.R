# Ignored
myCondition <- function(message) structure(list(message=message, call=NULL), class=c("myCondition", "condition"))

handle_myCondition <- function(e) {
  print(paste("enter handle_myCondition", e))
  invokeRestart("continue_test")
  print("after restart")
}

fun0 <- function(code) {
      withCallingHandlers({
      print(code)
      eval(code)
      },
      myCondition = handle_myCondition
    ); 
    print("exit fun0") 
}

fun1 <- function(s) {
	print(paste("enter fun1", s))
	withRestarts(
		{ signalCondition(myCondition(paste0("signal", s))); print("afterSignal") } ,
		continue_test = function(e) print("continue")
	)
	print(paste("exit fun1", s))
	NULL
}

fun0({
	fun1("first")
	fun1("second")
})
