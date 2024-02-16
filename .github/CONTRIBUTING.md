## Contributing

The project is in maintenance mode, meaning, changes are driven by contributed patches.
Only bug fixes. The Barcode Scanner app can
no longer be published, so almost no fixes will be accepted for it.
There is otherwise no development or roadmap for this project.

Only proposed changes in the form of a pull request are likely to be acted on, but,
these will be acted on promptly.

### Feature Requests

Feature requests are not accepted.

### Bug Reports

Bug reports must have enough detail to understand and reproduce the problem.
Bug reports without an associated pull request will generally be closed.
However, bug reports with a pull request are likely to be merged promptly.

## FAQ

Please search previous issues for an answer before opening a pull request. A few common ones
are listed here.

### This doesn't work on later Androids

The Barcode Scanner app cannot be updated or published further, and will not be updated.
It does not work on Android 14+ and will not.

### I get a compilation error

While you can check the build status on Github to confirm,
the project correctly builds and passes tests at all times.
90% of the time it's due to using an old version of Java. Version 3.4+ require Java 8.
Use earlier versions with Java 7 and earlier.

### This barcode doesn't decode

Not all images will decode. All else equal, more is better, but this is not accepted as a bug
report. A pull request that makes changes to make the barcode decode without decreasing the net
number of barcodes recognized in the unit tests may be considered.

## Licensing

Contributions via GitHub pull requests are gladly accepted from their original author.
Along with any pull requests, please state that the contribution is your original work and
that you license the work to the project under the project's open source license.
Whether or not you state this explicitly, by submitting any copyrighted material via
pull request, email, or other means you agree to license the material under the project's
open source license and warrant that you have the legal authority to do so.
