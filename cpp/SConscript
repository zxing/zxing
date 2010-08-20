Decider('MD5')

vars = Variables()
vars.Add(BoolVariable('DEBUG', 'Set to disable optimizations', 1))
vars.Add(BoolVariable('PIC', 'Set to 1 for to always generate PIC code', 0))
env = Environment(variables = vars)

debug = env['DEBUG']
compile_options = {}
flags = []
if debug:
	#compile_options['CPPDEFINES'] = "-DDEBUG"
	flags.append("-O0 -g3 -ggdb -Wall")
else:
	flags.append("-O -g3 -Wall")
if env['PIC']:
	flags.append("-fPIC")

compile_options['CXXFLAGS'] = ' '.join(flags)


def all_files(dir, ext='.cpp', level=5):
	files = []
	for i in range(1, level):
		files += Glob(dir + ('/*' * i) + ext) 
	return files



magick_include = ['/usr/include/ImageMagick/']
magick_libs = ['Magick++', 'MagickWand', 'MagickCore']

cppunit_libs = ['cppunit']

zxing_files = all_files('core/src')

zxing_include = ['core/src']
zxing_libs = env.Library('zxing', source=zxing_files, CPPPATH=zxing_include, **compile_options)

app_files = ['magick/src/MagickBitmapSource.cpp', 'magick/src/main.cpp']
app_executable = env.Program('zxing', app_files, CPPPATH=magick_include + zxing_include, LIBS=magick_libs + zxing_libs, **compile_options)

test_files = all_files('core/tests/src')
test_executable = env.Program('testrunner', test_files, CPPPATH=zxing_include, LIBS=zxing_libs + cppunit_libs, **compile_options)


Alias('lib', zxing_libs)
Alias('tests', test_executable)
Alias('zxing', app_executable)

