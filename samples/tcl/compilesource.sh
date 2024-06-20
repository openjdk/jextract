jextract --output src -l tcl -t org.tcl "<tcl.h>"

javac --source=22 -d . src/org/tcl/*.java
