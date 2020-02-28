package mypackage;

import java.util.List;
import java.lang.Comparable;

public class TestInnerClasses {

	
    public void topLevelMethod() {
        
    }
    
    public void topLevelMethod(List<? extends Comparable<?>> l) {
        
    }

	public static void topLevelStaticMethod() {
			
	}
	
	
	private String[] topLevelMethod1(int a, int b, int c) {

		Object oAnonInnerClass = new Object () {
			private String var = "abcd";
			
			public Object get() {
				return "Hello";
			}
		};
		
		
		class LocalInnerClass {
			public void innerClassMethod() {
				
			}
		}
		

		Object oAnonInnerClass2 = new Object () {
			private String var = "wxyz";
			
			public Object get() {
				return "Hello 2 you";
			}
			
			Object oAnonInnerInnerClass = new Comparable() {
			    public int compareTo(Object other) {
			        return 0;
			    }
			};
			
			class LocalInnerClassWithinAnonInnerClass {
				public void methodLA() {
					
				}
			}
			
			public void doNothing() {
			    
			}
		};
		
		return null;
	}
	
	private void topLevelMethod2() {
		Object anonInnerClass3 = new Object() {
			public void method2() {}
		};
		
        new TestInnerClassB("", new Runnable() {
            public void run() {
                // do something;
            }
        }).toString();
	}
	
	public class TestInnerClassA {
		private String varA;
		
		public String toString() {
			return "somethingA"; 
		}
		
		public void methodA() {
			Object oNestedAnonInner = new Object() {}; 
		}
	}
	
	public static class TestInnerClassB {
		private String varB;
		
		// for testing above...
        public TestInnerClassB(String s, Object o) {
            
        }
		
        public String toString() {
            return "somethingB"; 
        }
		
		public void methodB() {
			Object oNestedAnonInner = new Object() {}; 
		}
	}
}
