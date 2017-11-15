eval(expression({
renderPlot <- function(expr, width='auto', height='auto', res=72, ...,
                       env=parent.frame(), quoted=FALSE,
                       execOnResize=FALSE, outputArgs=list()
) {

  ## TODO: always exec until the display list is re-enabled
  execOnResize=TRUE
	
  # This ..stacktraceon is matched by a ..stacktraceoff.. when plotFunc
  # is called
  installExprFunction(expr, "func", env, quoted, ..stacktraceon = TRUE)

  args <- list(...)

  if (is.function(width))
    widthWrapper <- reactive({ width() })
  else
    widthWrapper <- function() { width }

  if (is.function(height))
    heightWrapper <- reactive({ height() })
  else
    heightWrapper <- function() { height }

  # A modified version of print.ggplot which returns the built ggplot object
  # as well as the gtable grob. This overrides the ggplot::print.ggplot
  # method, but only within the context of renderPlot. The reason this needs
  # to be a (pseudo) S3 method is so that, if an object has a class in
  # addition to ggplot, and there's a print method for that class, that we
  # won't override that method. https://github.com/rstudio/shiny/issues/841
  print.ggplot <- function(x) {
    grid::grid.newpage()

    build <- ggplot2::ggplot_build(x)

    gtable <- ggplot2::ggplot_gtable(build)
    grid::grid.draw(gtable)

    structure(list(
      build = build,
      gtable = gtable
    ), class = "ggplot_build_gtable")
  }


  getDims <- function() {
    width <- widthWrapper()
    height <- heightWrapper()

    # Note that these are reactive calls. A change to the width and height
    # will inherently cause a reactive plot to redraw (unless width and
    # height were explicitly specified).
    if (width == 'auto')
      width <- session$clientData[[paste0('output_', outputName, '_width')]]
    if (height == 'auto')
      height <- session$clientData[[paste0('output_', outputName, '_height')]]

    list(width = width, height = height)
  }

  # Vars to store session and output, so that they can be accessed from
  # the plotObj() reactive.
  session <- NULL
  outputName <- NULL

  # This function is the one that's returned from renderPlot(), and gets
  # wrapped in an observer when the output value is assigned. The expression
  # passed to renderPlot() is actually run in plotObj(); this function can only
  # replay a plot if the width/height changes.
  renderFunc <- function(shinysession, name, ...) {
    session <<- shinysession
    outputName <<- name

    dims <- getDims()

    if (is.null(dims$width) || is.null(dims$height) ||
        dims$width <= 0 || dims$height <= 0) {
      return(NULL)
    }

    # The reactive that runs the expr in renderPlot()
    plotData <- plotObj()

    img <- plotData$img

    # If only the width/height have changed, simply replay the plot and make a
    # new img.
    if (dims$width != img$width || dims$height != img$height) {
      pixelratio <- session$clientData$pixelratio %OR% 1

      coordmap <- NULL
      plotFunc <- function() {
     	## TODO: display list
        #..stacktraceon..(grDevices::replayPlot(plotData$recordedPlot))

        # Coordmap must be recalculated after replaying plot, because pixel
        # dimensions will have changed.

        if (inherits(plotData$plotResult, "ggplot_build_gtable")) {
          coordmap <<- getGgplotCoordmap(plotData$plotResult, pixelratio, res)
        } else {
          coordmap <<- getPrevPlotCoordmap(dims$width, dims$height)
        }
      }
      outfile <- ..stacktraceoff..(
        plotPNG(plotFunc, width = dims$width*pixelratio, height = dims$height*pixelratio,
                res = res*pixelratio)
      )
      on.exit(unlink(outfile))

      img <- dropNulls(list(
        src = session$fileUrl(name, outfile, contentType='image/png'),
        width = dims$width,
        height = dims$height,
        coordmap = coordmap,
        # Get coordmap error message if present
        error = attr(coordmap, "error", exact = TRUE)
      ))
    }

    img
  }


  plotObj <- reactive(label = "plotObj", {
    if (execOnResize) {
      dims <- getDims()
    } else {
      isolate({ dims <- getDims() })
    }

    if (is.null(dims$width) || is.null(dims$height) ||
        dims$width <= 0 || dims$height <= 0) {
      return(NULL)
    }

    # Resolution multiplier
    pixelratio <- session$clientData$pixelratio %OR% 1

    plotResult <- NULL
    recordedPlot <- NULL
    coordmap <- NULL
    plotFunc <- function() {
      success <-FALSE
      tryCatch(
        {
          # This is necessary to enable displaylist recording
          #grDevices::dev.control(displaylist = "enable")

          # Actually perform the plotting
          result <- withVisible(func())
          success <- TRUE
        },
        finally = {
          if (!success) {
            # If there was an error in making the plot, there's a good chance
            # it's "Error in plot.new: figure margins too large". We need to
            # take a reactive dependency on the width and height, so that the
            # user's plotting code will re-execute when the plot is resized,
            # instead of just replaying the previous plot (which errored).
            getDims()
          }
        }
      )

      if (result$visible) {
        # Use capture.output to squelch printing to the actual console; we
        # are only interested in plot output
        utils::capture.output({
          # This ..stacktraceon.. negates the ..stacktraceoff.. that wraps
          # the call to plotFunc. The value needs to be printed just in case
          # it's an object that requires printing to generate plot output,
          # similar to ggplot2. But for base graphics, it would already have
          # been rendered when func was called above, and the print should
          # have no effect.
          plotResult <<- ..stacktraceon..(print(result$value))
        })
      }

	  ## TODO: display list
      #recordedPlot <<- grDevices::recordPlot()

      if (inherits(plotResult, "ggplot_build_gtable")) {
        coordmap <<- getGgplotCoordmap(plotResult, pixelratio, res)
      } else {
        coordmap <<- getPrevPlotCoordmap(dims$width, dims$height)
      }
    }

    # This ..stacktraceoff.. is matched by the `func` function's
    # wrapFunctionLabel(..stacktraceon=TRUE) call near the beginning of
    # renderPlot, and by the ..stacktraceon.. in plotFunc where ggplot objects
    # are printed
    outfile <- ..stacktraceoff..(
      do.call(plotPNG, c(plotFunc, width=dims$width*pixelratio,
        height=dims$height*pixelratio, res=res*pixelratio, args))
    )
    on.exit(unlink(outfile))

    list(
      # img is the content that gets sent to the client.
      img = dropNulls(list(
        src = session$fileUrl(outputName, outfile, contentType='image/png'),
        width = dims$width,
        height = dims$height,
        coordmap = coordmap,
        # Get coordmap error message if present.
        error = attr(coordmap, "error", exact = TRUE)
      )),
      # Returned value from expression in renderPlot() -- may be a printable
      # object like ggplot2. Needed just in case we replayPlot and need to get
      # a coordmap again.
      plotResult = plotResult,
      recordedPlot = recordedPlot
    )
  })


  # If renderPlot isn't going to adapt to the height of the div, then the
  # div needs to adapt to the height of renderPlot. By default, plotOutput
  # sets the height to 400px, so to make it adapt we need to override it
  # with NULL.
  outputFunc <- plotOutput
  if (!identical(height, 'auto')) formals(outputFunc)['height'] <- list(NULL)

  markRenderFunction(outputFunc, renderFunc, outputArgs = outputArgs)

}
}), asNamespace("shiny"))