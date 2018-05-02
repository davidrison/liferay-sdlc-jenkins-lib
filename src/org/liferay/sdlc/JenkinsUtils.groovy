import java.util.LinkedList;
import jenkins.model.*;
import hudson.plugins.sshslaves.SSHLauncher
import hudson.slaves.DumbSlave
import hudson.slaves.RetentionStrategy
import hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy;
import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy
import static com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy.PROJECT;

static def createProjectFromXML(jobName, jobXml)
{
    Jenkins.instance.createProjectFromXML(jobName, new ByteArrayInputStream(jobXml.getBytes()))
}

static def checkViewExists(viewName)
{
    return Jenkins.instance.getView(viewName) != null;
}

static def createView(viewName)
{
    Jenkins.instance.addView(new ListView(viewName, Jenkins.instance))
}

static def addJobToView(jobName, viewName)
{
    Jenkins.instance.getView(viewName).add(Jenkins.instance.getItem(jobName))
}

static def assignSidToRole(sid, roleName) {
    def projectRoleMap = Jenkins.instance.authorizationStrategy.roleMaps.get(PROJECT);

    def role = projectRoleMap.getRole(roleName);

    try {
        if (role == null) {
            println "It's not possible to team role because the required role doesn't exist"
            return;
        }
        println "Assign $sid to role $roleName"
        projectRoleMap.assignRole(role, sid);
    }finally {
        projectRoleMap = null;
        role = null;
    }
}

static def getJobParameterNames(jobName) {
	def parametersNames= []
    for (e in Jenkins.instance.getItem(jobName).properties) {
        if (e.key instanceof hudson.model.ParametersDefinitionProperty.DescriptorImpl) {
            parametersNames = e.value.parameterDefinitionNames
        }
    }
	return parametersNames;
}

static def createJenkinsSlave(name, description, hostname, privateKeyId) {
    if (Jenkins.instance.getNode(name) != null) {
        println "Node $name already exists"
        return;
    }
    DumbSlave slave = new DumbSlave(name, description, 
        "/home/jenkins/.jenkins",
        "1",
        Node.Mode.NORMAL,
        "",
        new SSHLauncher(hostname, 22, privateKeyId,"","","","",null,null,null, new NonVerifyingKeyVerificationStrategy()),
        new RetentionStrategy.Always(),
        new LinkedList());
  
    Jenkins.instance.addNode(slave);
}
