@ignore
Feature: Utility Function Library
  Background:
    * def RDFUtils = Java.type('org.solid.testharness.utils.RDFUtils')
    * def SolidResource = Java.type('org.solid.testharness.utils.SolidResource')
    * def SolidContainer = Java.type('org.solid.testharness.utils.SolidContainer')

  Scenario:
    * def statusSuccess = function(){ const status = karate.get('responseStatus'); return status >= 200 && status < 300 }
    * def statusRedirect = function(){ const status = karate.get('responseStatus'); return status >= 300 && status < 400 }
    * def statusFail = function(){ const status = karate.get('responseStatus'); return status >= 400 && status < 500 }
    * def parseWacAllowHeader = function(headers) { return Java.type('org.solid.testharness.http.HttpUtils').parseWacAllowHeader(headers) }

    * def createTestContainer =
    """
      function(client) {
        const container = SolidContainer.create(client, target.serverRoot + target.testContainer).generateChildContainer();
        karate.log(container);
        return container;
      }
    """
    * def createOwnerAuthorization =
    """
      function(ownerAgent, target) {
        return `
<#owner> a acl:Authorization;
  acl:agent <${ownerAgent}>;
  acl:accessTo <${target}>;
  acl:default <${target}>;
  acl:mode acl:Read, acl:Write, acl:Control.`
      }
    """
    * def createAuthorization =
    """
      function(config) {
        // config: { authUri, agents, groups, publicAccess, authenticatedAccess, accessToTargets, defaultTargets, modes }
        let acl = `\n<${config.authUri}> a acl:Authorization;`
        if ('agents' in config) {
          if (Array.isArray(config.agents)) {
            acl += config.agents.map((agent) => `\n  acl:agent <${agent}>;`)
          } else {
            acl += `\n  acl:agent <${config.agents}>;`
          }
        }
        if ('groups' in config) {
          if (Array.isArray(config.groups)) {
            acl += config.groups.map((group) => `\n  acl:agentGroup <${group}>;`)
          } else {
            acl += `\n  acl:agentGroup <${config.groups}>;`
          }
        }
        if (config.publicAccess) {
          acl += '\n  acl:agentClass foaf:Agent;'
        }
        if (config.authenticatedAccess) {
          acl += '\n  acl:agentClass acl:AuthenticatedAgent;'
        }
        if ('accessToTargets' in config) {
          if (Array.isArray(config.accessToTargets)) {
            acl += config.accessToTargets.map((target) => `\n  acl:accessTo <${target}>;`)
          } else {
            acl += `\n  acl:accessTo <${config.accessToTargets}>;`
          }
        }
        if ('defaultTargets' in config) {
          if (Array.isArray(config.defaultTargets)) {
            acl += config.defaultTargets.map((target) => `\n  acl:default <${target}>;`)
          } else {
            acl += `\n  acl:default <${config.defaultTargets}>;`
          }
        }
        if (Array.isArray(config.modes)) {
          acl += `\n  acl:mode ${config.modes.join(', ')}.`
        } else if (config.modes) {
          acl += `\n  acl:mode ${config.modes}.`
        }
        return acl
      }
    """
    * def aclPrefix = '@prefix acl: <http://www.w3.org/ns/auth/acl#>.\n@prefix foaf: <http://xmlns.com/foaf/0.1/>.'
    * def createBobAccessToAuthorization =
    """
      function(webID, resourcePath, modes) {
        return createAuthorization({authUri: '#bobAccessTo', agents: webID, accessToTargets: resourcePath, modes: modes})
      }
    """
    * def createBobDefaultAuthorization =
    """
      function(webID, resourcePath, modes) {
        return createAuthorization({authUri: '#bobDefault', agents: webID, defaultTargets: resourcePath, modes: modes})
      }
    """
    * def createPublicAccessToAuthorization =
    """
      function(resourcePath, modes) {
        return createAuthorization({authUri: '#publicAccessTo', publicAccess: true, accessToTargets: resourcePath, modes: modes})
      }
    """
    * def createPublicDefaultAuthorization =
    """
      function(resourcePath, modes) {
        return createAuthorization({authUri: '#publicDefault', publicAccess: true, defaultTargets: resourcePath, modes: modes})
      }
    """

    * def clientAlice = Java.type('org.solid.testharness.http.AuthManager').authenticate('alice', target)
    * def clientBob = Java.type('org.solid.testharness.http.AuthManager').authenticate('bob', target)
    * def authenticate =
    """
      function(user) {
        if (user == 'bob') return clientBob;
        else return clientAlice;
      }
    """

    * def pause = function (pause) { java.lang.Thread.sleep(pause) }
