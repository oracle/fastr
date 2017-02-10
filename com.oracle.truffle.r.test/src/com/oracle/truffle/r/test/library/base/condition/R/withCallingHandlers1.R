# Ignored
cond0 <- function(message)
  structure(list(message=message, call=NULL), class=c("cond0", "condition"))

cond1 <- function(message)
  structure(list(message=message, call=NULL), class=c("cond1", "condition"))

handle_cond0 <- function(e) {
  print(paste("enter handle_cond0", e))
  invokeRestart("continue_test")
  print("after cond0 restart")
}

handle_cond1 <- function(e) {
  print(paste("enter handle_cond1", e))
  signalCondition(e)
  print("after cond1 restart")
}

fun0 <- function(code) {
      withCallingHandlers({
      print(code)
      eval(code)
      },
      cond0 = handle_cond0,
      cond1 = handle_cond1
    ); 
    print("exit fun0") 
}

fun1 <- function(s) {
	print(paste("enter fun1", s))
	withRestarts(
		{ signalCondition(cond1(paste0("signal", s)));signalCondition(cond0(paste0("signal", s))); print("afterSignal") } ,
		continue_test = function(e) print("continue")
	)
	print(paste("exit fun1", s))
	NULL
}

fun0({
	fun1("first")
	fun1("second")
})

