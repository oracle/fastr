package org.rosuda.JRI;

/** Interface which must be implmented by any class that wants to pose as the call-back handler for R event loop callbacks. It is legal to return immediately except when user interaction is required: {@link #rReadConsole} and {@link #rChooseFile} are expected to block until the user performs the desired action. */
public interface RMainLoopCallbacks {
    /** called when R prints output to the console
	@param re calling engine
	@param text text to display in the console
	@param oType output type (0=regular, 1=error/warning)
    */
    public void   rWriteConsole (Rengine re, String text, int oType);
    /** called when R enters or exits a longer evaluation. It is usually a good idea to signal this state to the user, e.g. by changing the cursor to a "hourglass" and back.
	@param re calling engine
	@param which identifies whether R enters (1) or exits (0) the busy state */
    public void   rBusy         (Rengine re, int which);
    /** called when R waits for user input. During the duration of this callback it is safe to re-enter R, and very often it is also the only time. The implementation is free to block on this call until the user hits Enter, but in JRI it is a good idea to call {@link Rengine.rniIdle()} occasionally to allow other event handlers (e.g graphics device UIs) to run. Implementations should NEVER return immediately even if there is no input - such behavior will result in a fast cycling event loop which makes the use of R pretty much impossible.
	@param re calling engine
	@param prompt prompt to be displayed at the console prior to user's input
	@param addToHistory flag telling the handler whether the input should be considered for adding to history (!=0) or not (0)
	@return user's input to be passed to R for evaluation */
    public String rReadConsole  (Rengine re, String prompt, int addToHistory);
    /** called when R want to show a warning/error message (not to be confused with messages displayed in the console output)
	@param re calling engine
	@param message message to display */
    public void   rShowMessage  (Rengine re, String message);
    /** called when R expects the user to choose a file
	@param re calling engine
	@param newFile flag determining whether an existing or new file is to be selecteed
	@return path/name of the selected file */
    public String rChooseFile   (Rengine re, int newFile);
    /** called when R requests the console to flush any buffered output
	@param re calling engine */	
    public void   rFlushConsole (Rengine re);
    /** called to save the contents of the history (the implementation is responsible of keeping track of the history)
	@param re calling engine
	@param filename name of the history file */
    public void   rSaveHistory  (Rengine re, String filename);
    /** called to load the contents of the history
	@param re calling engine
	@param filename name of the history file */
    public void   rLoadHistory  (Rengine re, String filename);
}
