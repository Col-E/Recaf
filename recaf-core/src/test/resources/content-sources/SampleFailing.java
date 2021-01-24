public class Sample {
	public static void main(String[] args) // Missing {
		System.out.println("Sample");
	}
}
/*
javac SampleFailing.java

SampleFailing.java:2: error: ';' expected
        public static void main(String[] args)
                                              ^
SampleFailing.java:5: error: class, interface, or enum expected
}
^
2 errors
*/