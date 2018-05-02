package org.liferay.sdlc;

import static org.liferay.sdlc.SDLCPrUtilities.*

def _gradlew(args)
{
    if (isUnix()) {
        sh "./gradlew --no-daemon " + args
        if ("initBundle".equals(args))
            sh 'chmod +x . $(find . -name catalina.sh)'
    }
    else
        bat "gradlew --no-daemon " + args
}

def _withCredentials(credentialsId, userVar, passwordVar, body)
{
    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: credentialsId, usernameVariable: userVar, passwordVariable: passwordVar]]) {
        body()
    }
}

@NonCPS
def log(args) {
    println args
}

def loadLibrary(resource) {
   def contents = libraryResource resource;
   return contents
}

// Must not be NonCps
def _readFile(fileName) {
    return readFile(fileName)
}

// Must not be NonCps
def _fileExists(fileName) {
    return fileExists(fileName);
}

// Must not be NonCps
def _writeFile(fileName, contents) {
    writeFile file: fileName, text: contents
}

@NonCPS
def _parseStringToJson(str) {
    return new groovy.json.JsonSlurperClassic().parseText(str)
}

@NonCPS
def _getJsonFromStringUsingPattern(str, pattern) {
    def matcher = (str =~ pattern)
    return matcher ? _parseStringToJson(matcher.group(0)) : null
}

@NonCPS
def isSonarVerificationEnabled() {
    return env.ENABLE_SONAR == "true";
}

@NonCPS
def SonarHostUrl() {
    return env.SONAR_END_POINT;
}

@NonCPS
def ChangeId() {
    return env.CHANGE_ID;
}

@NonCPS
def GithubOauth() {
    return env.GithubOauth;
}

def closePullRequest(gitRepository, emailLeader, gitAuthentication) {
    def CHANGE_ID = ChangeId();

    def body = """
    {"state": "closed"}
    """
    httpRequest acceptType: 'APPLICATION_JSON', authentication: "${gitAuthentication}", contentType: 'APPLICATION_JSON', httpMode: 'PATCH', requestBody: body, url: "https://api.github.com/repos/${gitRepository}/pulls/${CHANGE_ID}"            

    def response = httpRequest acceptType: 'APPLICATION_JSON', authentication: "${gitAuthentication}", contentType: 'APPLICATION_JSON', httpMode: 'GET', url: "https://api.github.com/repos/${gitRepository}/pulls/${CHANGE_ID}"
    def login = getLogin(response.content)

    def respUser = httpRequest acceptType: 'APPLICATION_JSON', authentication: "${gitAuthentication}", contentType: 'APPLICATION_JSON', httpMode: 'GET', url: "https://api.github.com/users/${login}"
    def email = getEmail(respUser.content)
    def emailText = 'Your Pull Request PR-${CHANGE_ID} broke the build and will be removed. Please fix it at your earliest convenience and re-submit. ${JOB_URL}'
    def emailSubject = "Validate PR-${CHANGE_ID}"
   
    emailext body: "${emailText}", subject: "${emailSubject}", to: "${email}"
    emailext body: "${emailText}", subject: "${emailSubject}", to: "${emailLeader}"
}
