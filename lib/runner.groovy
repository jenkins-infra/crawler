#!/usr/bin/env groovy
System.setProperty("mavenVersion","3.0")
print "loading dependencies..."
Class.forName("lib.init",true,this.class.classLoader)

if (args.length==0) {
    println "No script specified";
    System.exit(-1);
} else {
    args.each { arg ->
//        if (arg.endsWith(".groovy")) arg = arg.substring(0,arg.length()-7);
//        Class.forName(arg,true,this.class.classLoader).newInstance().run();
        evaluate(new File(arg))
    }
}
