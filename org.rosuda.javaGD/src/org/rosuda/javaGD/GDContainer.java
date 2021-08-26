//
//  Java Graphics Device
//
//  Created by Simon Urbanek on Thu Aug 05 2004.
//  Copyright (c) 2004-2009 Simon Urbanek. All rights reserved.
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation;
//  version 2.1 of the License.
//
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public
//  License along with this library; if not, write to the Free Software
//  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
//
package org.rosuda.javaGD;

import java.awt.*;
import java.util.Collection;

/**
 * <code>GDContainer</code> is the minimal interface that has to be implemented by classes that are
 * used as back-ends for JavaGD. The interface feeds graphics objects to the instance which are then
 * free to use them for any purpose such as display or printing.
 */
public interface GDContainer {
    /**
     * add a new plot object to the list
     *
     * @param o plot object
     */
    public void add(GDObject o);

    /**
     * Returns all GDObjects located in this container.
     * fastr-specific method.
     * @return All GDObjects there have been added to this container.
     */
    public Collection<GDObject> getGDObjects();

    /**
     * reset the plot- remove all objects
     *
     * @param pageNumber explicit page number. Ignore if negative. Used in tests.
     */
    public void reset(int pageNumber);

    /**
     * retrieve graphics state
     *
     * @return current graphics state
     */
    public GDState getGState();

    /**
     * retrieve graphics if this container is backed by some {@link Graphics} object.
     *
     * @return current graphics object or <code>null</code> if this container is not associated with
     *         any
     */
    public Graphics getGraphics(); // implementation is free to return null
    // public void repaint();
    // public void repaint(long tm);

    /**
     * this method is called to notify the contained that a locator request is pending; the
     * container must either return <code>false</code> and ignore the <code>ls</code> parameter *or*
     * return <code>true</code> and call @link{LocatorSync.triggerAction} method at some point in
     * the future (which may well be after returning from this method)
     *
     * @param ls locator synchronization object
     */
    public boolean prepareLocator(LocatorSync ls);

    /**
     * synchronize display with the graphics objects
     *
     * @param finish flag denoting whether the synchronization is desired or not (<code>true</code>
     *            for a finished batch, <code>false</code> when a batch starts)
     */
    public void syncDisplay(boolean finish);

    /**
     * set the device number of this container
     *
     * @param dn device number
     */
    public void setDeviceNumber(int dn);

    /** close the display associated with this container */
    public void closeDisplay();

    /**
     * retrieve the current device number
     *
     * @return current device number
     */
    public int getDeviceNumber();

    /**
     * retrieve the size of the container
     *
     * @return size of the container
     */
    public Dimension getSize();
}
