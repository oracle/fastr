//
//  LocatorSync.java
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

/** a simple synchronization class that can be used by a separate thread to wake JavaGD from waiting for a locator result. The waiting thread calls {@link #waitForAction()} which returns only after another thread calls {@link #triggerAction}. */
public class LocatorSync {
    private double[] locResult=null;
    private boolean notificationArrived=false;

    /** this internal method waits until {@link #triggerAction} is called by another thread. It is implemented by using {@link #wait()} and checking {@link #notificationArrived}.
     * @return result supplied when {@link #triggerAction} was called - essentially the retuls to be returned by locator
     */
    public synchronized double[] waitForAction() {
        while (!notificationArrived) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
	notificationArrived=false;
	return locResult;
    }

    /** this methods awakens {@link #waitForAction()}. It is implemented by setting {@link #notificationArrived} to <code>true</code>, setting {@link #locResult} to the passed result and finally calling {@link #notifyAll()}.
     * @param result result to pass to {@link #waitForAction()}
     */
    public synchronized void triggerAction(double[] result) {
	locResult=result;
        notificationArrived=true;
        notifyAll();
    }
}
