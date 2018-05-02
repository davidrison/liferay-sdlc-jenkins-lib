package org.liferay.sdlc;

import jenkins.model.*;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.DirectEntryPrivateKeySource;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

def credentialsExists(credentialsId)
{
    domains=SystemCredentialsProvider.instance.getDomainCredentials().domain;
    for (d in domains) {
        for (it in SystemCredentialsProvider.getInstance().getStore().getCredentials(d)) {
            if (it.id == credentialsId)
                return true;
        }   
    }
    return false;
}

def createSshPrivateKey(credentialsId, description, username, privateKey, passphrase)
{
    addToCredentialsDomain(new BasicSSHUserPrivateKey(CredentialsScope.GLOBAL, credentialsId, username, new DirectEntryPrivateKeySource(privateKey), passphrase, description))
}

def createUsernameWithPasswordCredentials(credentialsId, description, username, password)
{
    addToCredentialsDomain(new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, description, username, password))
}

def addToCredentialsDomain(credential) {
    d = SystemCredentialsProvider.instance.getDomainCredentials().domain.get(0);
    SystemCredentialsProvider.getInstance().getStore().addCredentials(d, credential)
}

def getLoginFor(credentialsId)
{
    domains=SystemCredentialsProvider.instance.getDomainCredentials().domain;
    for (d in domains) {
        for (it in SystemCredentialsProvider.getInstance().getStore().getCredentials(d)) {
            if (it.id == credentialsId)
                return it.username
        }   
    }
    return null;
}


def createSshPrivateKeyIfNeeded(credId, missingMessage, description) 
{
    if (!credentialsExists(credId)) {
        println missingMessage
        def serverData = input(
            id: 'serverData', 
            message: 'Provide ssh credentials to access the server', 
            requestTimeout: 300,
            parameters: [
                [$class: 'StringParameterDefinition', description: 'username', name: 'username'],
                [$class: 'TextParameterDefinition', description: 'private key', name: 'privateKey']
                ]
        )
        
        createSshPrivateKey(credId, description, serverData['username'], serverData['privateKey'], "")
    }
}

def createSshPrivateKeyWithPassprhaseIfNeeded(credId, missingMessage, description) 
{
    if (!credentialsExists(credId)) {
        println missingMessage
        def serverData = input(
            id: 'serverData', 
            message: 'Provide ssh credentials to access the server', 
            requestTimeout: 300,
            parameters: [
                [$class: 'StringParameterDefinition', description: 'username', name: 'username'],
                [$class: 'TextParameterDefinition', description: 'private key', name: 'privateKey'],
                [$class: 'PasswordParameterDefinition', description: 'passphrase', name: 'passphrase']
                ]
        )
        
        createSshPrivateKey(credId, description, serverData['username'], serverData['privateKey'], serverData['passphrase'].plainText)
    }
}
