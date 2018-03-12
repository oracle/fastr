//
// PlotDemo demo - REngine and graphics
//
// $Id$
//

import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;

import org.rosuda.REngine.*;
import org.rosuda.REngine.Rserve.*;

/** A demonstration of the use of Rserver and graphics devices to create graphics in R, pull them into Java and display them. It is a really simple demo. */
public class PlotDemo extends Canvas {
    public static void main(String args[]) {
		try {
			String device = "jpeg"; // device we'll call (this would work with pretty much any bitmap device)
			
			// connect to Rserve (if the user specified a server at the command line, use it, otherwise connect locally)
			RConnection c = new RConnection((args.length>0)?args[0]:"127.0.0.1");

			// if Cairo is installed, we can get much nicer graphics, so try to load it
			if (c.parseAndEval("suppressWarnings(require('Cairo',quietly=TRUE))").asInteger()>0)
				device="CairoJPEG"; // great, we can use Cairo device
			else
				System.out.println("(consider installing Cairo package for better bitmap output)");
			
            // we are careful here - not all R binaries support jpeg
            // so we rather capture any failures
            REXP xp = c.parseAndEval("try("+device+"('test.jpg',quality=90))");
            
            if (xp.inherits("try-error")) { // if the result is of the class try-error then there was a problem
                System.err.println("Can't open "+device+" graphics device:\n"+xp.asString());
                // this is analogous to 'warnings', but for us it's sufficient to get just the 1st warning
                REXP w = c.eval("if (exists('last.warning') && length(last.warning)>0) names(last.warning)[1] else 0");
                if (w.isString()) System.err.println(w.asString());
                return;
            }
                 
            // ok, so the device should be fine - let's plot - replace this by any plotting code you desire ...
            c.parseAndEval("data(iris); attach(iris); plot(Sepal.Length, Petal.Length, col=unclass(Species)); dev.off()");
            
			// There is no I/O API in REngine because it's actually more efficient to use R for this
			// we limit the file size to 1MB which should be sufficient and we delete the file as well
			xp = c.parseAndEval("r=readBin('test.jpg','raw',1024*1024); unlink('test.jpg'); r");
			
            // now this is pretty boring AWT stuff - create an image from the data and display it ...
            Image img = Toolkit.getDefaultToolkit().createImage(xp.asBytes());
            
            Frame f = new Frame("Test image");
            f.add(new PlotDemo(img));
            f.addWindowListener(new WindowAdapter() { // just so we can close the window
                public void windowClosing(WindowEvent e) { System.exit(0); }
            });
            f.pack();
            f.setVisible(true);

			// close RConnection, we're done
			c.close();
        } catch (RserveException rse) { // RserveException (transport layer - e.g. Rserve is not running)
            System.out.println(rse);
        } catch (REXPMismatchException mme) { // REXP mismatch exception (we got something we didn't think we get)
            System.out.println(mme);
            mme.printStackTrace();
        } catch(Exception e) { // something else
            System.out.println("Something went wrong, but it's not the Rserve: "
							   +e.getMessage());
            e.printStackTrace();
        }
    }
	
    Image img;

    public PlotDemo(Image img) {
        this.img=img;
        MediaTracker mediaTracker = new MediaTracker(this);
        mediaTracker.addImage(img, 0);
        try {
            mediaTracker.waitForID(0);
        } catch (InterruptedException ie) {
            System.err.println(ie);
            System.exit(1);
        }
        setSize(img.getWidth(null), img.getHeight(null));
    }
	
    public void paint(Graphics g) {
        g.drawImage(img, 0, 0, null);
    }
}
