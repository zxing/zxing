/*
 * Copyright 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.bug.servicetracker;

import com.buglabs.application.AbstractServiceTracker;
import com.buglabs.bug.module.camera.pub.ICameraDevice;
import com.buglabs.bug.module.camera.pub.ICameraModuleControl;
import com.buglabs.bug.module.lcd.pub.IModuleDisplay;
import com.buglabs.device.IButtonEventProvider;
import com.google.zxing.client.bug.app.BugBarcodeApp;
import org.osgi.framework.BundleContext;

import java.awt.Frame;
import java.util.Collection;

/**
 * Service tracker for the BugApp Bundle
 *
 * @author David Albert
 */
public final class BugBarcodeServiceTracker extends AbstractServiceTracker {

  private IButtonEventProvider buttonEventProvider;
	private Frame frame;
	private BugBarcodeApp app;
	
	public BugBarcodeServiceTracker(BundleContext context) {
		super(context);
	}

	@Override
  public void doStart() {
    IModuleDisplay display = (IModuleDisplay) getService(IModuleDisplay.class);
    ICameraDevice camera = (ICameraDevice) getService(ICameraDevice.class);
    ICameraModuleControl cameraControl = (ICameraModuleControl) getService(ICameraModuleControl.class);
		buttonEventProvider = (IButtonEventProvider) getService(IButtonEventProvider.class);
		frame = display.getFrame();
		app = new BugBarcodeApp(frame, camera, cameraControl, buttonEventProvider);
	}

	/**
	 * Called when a service that this application depends is unregistered.
	 */
	@Override
  public void doStop() {
		buttonEventProvider.removeListener(app);
		frame.dispose();
	}

	/**
	 * Allows the user to set the service dependencies by
   * adding them to services list returned by getServices().
   * i.e.nl getServices().add(MyService.class.getName());
	 */
	@Override
  public void initServices() {
    Collection<String> appServices = (Collection<String>) getServices();
    appServices.add("com.buglabs.bug.module.camera.pub.ICameraDevice");
		appServices.add("com.buglabs.bug.module.lcd.pub.IModuleDisplay");
		appServices.add(IButtonEventProvider.class.getName());
		appServices.add(ICameraModuleControl.class.getName());
	}
	
}

