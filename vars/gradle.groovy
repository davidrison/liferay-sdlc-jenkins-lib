def call(args) {
    if (isUnix()) 
        sh "./gradlew " + args 
    else 
        bat "call gradlew " + args
}
