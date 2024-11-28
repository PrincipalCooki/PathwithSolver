test: compile
	java -cp pathwidth-1.0-all.jar:. edu.kit.iti.formal.pathwidth.PathwidthTest
compile:
	javac -cp pathwidth-1.0-all.jar MyPathwidthSolver.java

my_test: compile
	javac -cp MyTest.java