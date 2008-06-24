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

package com.google.zxing.client.bug;

import com.buglabs.util.ServiceFilterGenerator;
import com.google.zxing.client.bug.servicetracker.BugBarcodeServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * BundleActivator for BugBarcode.
 *
 * @author David Albert
 */
public final class Activator implements BundleActivator {

  private BugBarcodeServiceTracker barcodeServiceTracker;
	private ServiceTracker serviceTracker;

	public void start(BundleContext context) throws InvalidSyntaxException {
		barcodeServiceTracker = new BugBarcodeServiceTracker(context);
		Filter filter =
        context.createFilter(ServiceFilterGenerator.generateServiceFilter(barcodeServiceTracker.getServices()));
		serviceTracker = new ServiceTracker(context, filter, barcodeServiceTracker);
		serviceTracker.open();
	}

	public void stop(BundleContext context) {
		barcodeServiceTracker.stop();
		serviceTracker.close();
	}
}
