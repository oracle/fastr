eval(expression({
plotPNG <- function(func, filename=tempfile(fileext='.png'),
                    width=400, height=400, res=72, ...) {
                    
  # If quartz is available, use png() (which will default to quartz).
  # Otherwise, if the Cairo package is installed, use CairoPNG().
  # Finally, if neither quartz nor Cairo, use png().
  if (capabilities("aqua")) {
    pngfun <- grDevices::png
  } else if ((getOption('shiny.usecairo') %OR% TRUE) &&
             nchar(system.file(package = "Cairo"))) {
    pngfun <- Cairo::CairoPNG
  } else {
    pngfun <- grDevices::png
  }

  pngfun(filename=filename, width=width, height=height, res=res, ...)
  # Call plot.new() so that even if no plotting operations are performed at
  # least we have a blank background. N.B. we need to set the margin to 0
  # temporarily before plot.new() because when the plot size is small (e.g.
  # 200x50), we will get an error "figure margin too large", which is triggered
  # by plot.new() with the default (large) margin. However, this does not
  # guarantee user's code in func() will not trigger the error -- they may have
  # to set par(mar = smaller_value) before they draw base graphics.
  ## TODO:
  #op <- graphics::par(mar = rep(0, 4))
  #tryCatch(
    #graphics::plot.new(),
    #finally = graphics::par(op)
  #)

  dv <- grDevices::dev.cur()
  on.exit(grDevices::dev.off(dv), add = TRUE)
  func()

  filename
}
}), asNamespace("shiny"))