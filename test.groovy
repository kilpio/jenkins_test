#!/usr/bin/env groovy

// Parameters:
// BUILD_BRANCH: stable
// AUTO_MERGE_FROM_MASTER: false

// FABRIC_VERSION: 1.4.6

// GIT_REPO_OWNER: kilpio
// GITHUB_SSH_CREDENTIALS_ID: github

// DOCKER_REGISTRY: https://registry-1.docker.io/v2
// DOCKER_REPO: fs
// DOCKER_CREDENTIALS_ID: dockerhub





node {

    stage('Fabric-Starter-projects-Snapshot') {

        def newFabricStarterTag
        //Building fabric-tools-extended image
        stage('Fabric-tools-extended') {
            // git@github.com:${GIT_REPO_OWNER}/${repositoryName}.git 
            // Wiping out workspace first.
            // Cloning repository git@github.com:${GIT_REPO_OWNER}/fabric-starter.git
            // checkout 'master' and 'stable' branches 
            checkoutFromGithubToSubfolder('fabric-starter', "${BUILD_BRANCH}")
            //// sh("cp -r build/chaincode/node/dns fabric-starter/chaincode/node")
            
            // cd /var/jenkins_home/workspace/[_test_]/fabric-starter
            dir('fabric-starter') {
                if (AUTO_MERGE_FROM_MASTER) {
                    //                             
                    sh "git config user.name ${GIT_REPO_OWNER}"
                    // Switche to the branch 'master'
                    sh "git checkout master"
                    // Update local version from remote
                    sh "git pull"
                    // Switche to the branch 'stable'
                    sh "git checkout ${BUILD_BRANCH}"
                    // Update local version from remote
                    sh "git pull"
                    // Overwrite the contents of the files in '.' (overwrite working tree ('stable') with the contents in the index ('master'))
                    sh "git checkout master -- ."
                    // Commit changes (to the branch 'stable'), do not push to github yet
                    sh "git commit -m 'Stable branch along with ${newFabricStarterTag}' || true"
            }
                // Evaluate next snapshot name for Fabric-starter: ${branchPrefix}-${majorVer}.${minorVersion + 1}-${FABRIC_VERSION}
                newFabricStarterTag = evaluateNextSnapshotGitTag('Fabric-starter')
                // docker build -> fabric-tools-extended:newFabricStarterTag
                // push it as fabric-tools-extended:newFabricStarterTag and as fabric-tools-extended:latest
                buildAndPushDockerImage("fabric-tools-extended", newFabricStarterTag, "--no-cache --build-arg=FABRIC_VERSION=${FABRIC_VERSION} -f fabric-tools-extended/Dockerfile .")
            }
        }
        //Building fabric-starter-rest image
        stage("Build Stable docker image of fabric-starter-rest") {
            // Wiping out workspace first.
            // Cloning repository git@github.com:${GIT_REPO_OWNER}/fabric-starter-rest.git
            // checkout 'master' and 'stable' branches 
            checkoutFromGithubToSubfolder('fabric-starter-rest')
            
            // cd /var/jenkins_home/workspace/[_test_]/fabric-starter-rest
            dir('fabric-starter-rest') {
                // docker build -> fabric-starter-rest: newFabricStarterTag
                // push it as fabric-starter-rest:newFabricStarterTag and as fabric-starter-rest:latest
                buildAndPushDockerImage('fabric-starter-rest', newFabricStarterTag, "--no-cache -f Dockerfile .")
            }
        }

        stage('Fabric-Starter') {
            stage('Snapshot fabric-starter') {
////                checkoutFromGithubToSubfolder('fabric-starter')

                dir('fabric-starter') {
                    sshagent(credentials: ["${GITHUB_SSH_CREDENTIALS_ID}"]) {
                        sh "git checkout -B ${newFabricStarterTag}" // create new branch if it does not exist
                        sh "git config user.name ${GIT_REPO_OWNER}"
                        def fileContent = readFile '.env'           //changing .env file
                        //appending FABRIC_STARTER_VERSION and FABRIC_VERSION after the last line
                        writeFile file: '.env', text: "${fileContent}\nFABRIC_STARTER_VERSION=${newFabricStarterTag}\nFABRIC_VERSION=${FABRIC_VERSION}"
                        sh "git add .env"
                        // modifing yamls
                        // by replacing 'latest' with approps
                        updateComposeFilesWithVersions(FABRIC_VERSION, newFabricStarterTag)

                        sh "git commit -m 'Snapshot ${newFabricStarterTag}'"
                        // push to the remote newFabricStarterTag branch
                        // now newFabricStarterTag branch set up to track remote branch newFabricStarterTag from origin
                        sh("git push -u origin ${newFabricStarterTag}")

                        sh "git checkout ${BUILD_BRANCH}" //Switched to branch 'stable'
                        // We do not specify remote or branch here
                        sh "git push"
                    }
                }
            }
        }

        stage('Snapshot fabric-starter-rest') {
            //new snapshot branch for the fabric-starter-rest
            dir('fabric-starter-rest') {
                sshagent(credentials: ["${GITHUB_SSH_CREDENTIALS_ID}"]) {
                    sh "git checkout -B ${newFabricStarterTag}"
                    sh("git push -u origin ${newFabricStarterTag}")
                }
            }
        }
    }
}

private void buildAndPushDockerImage(imageName, tag, def args = '') {

    docker.withRegistry("${DOCKER_REGISTRY}", "${DOCKER_CREDENTIALS_ID}") {
//        def fabricRestImage = docker.build("${DOCKER_REPO}/${imageName}:tag", args)
        if (!args?.trim()) {
            args = "-f Dockerfile ."

        }
        args = "-t ${DOCKER_REPO}/${imageName}:${tag} ${args}"
        echo "docker build args: $args"
        sh "docker image build ${args}"
        fabricRestImage = docker.image("${DOCKER_REPO}/${imageName}:${tag}")
        fabricRestImage.push()
        fabricRestImage.push('latest')
    }
}


def checkoutFromGithubToSubfolder(repositoryName, def branch = 'master', def clean = true) {
    def extensions = [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${repositoryName}"],
                      [$class: 'UserIdentity', name: "${GIT_REPO_OWNER}"],
                      [$class: 'ScmName', name: "${GIT_REPO_OWNER}"]
    ]
    if (clean) {
        extensions.push([$class: 'WipeWorkspace']) //CleanBeforeCheckout
    }
    checkout([$class                           : 'GitSCM', branches: [ [name: '*/master'], [name: "*/${branch}"]],
              doGenerateSubmoduleConfigurations: false, submoduleCfg: [],
              userRemoteConfigs                : [[credentialsId: "${GITHUB_SSH_CREDENTIALS_ID}", url: "git@github.com:${GIT_REPO_OWNER}/${repositoryName}.git"]],
              extensions                       : extensions
    ])
}

def evaluateNextSnapshotGitTag(repositoryTitle) {
    echo "Evaluate next snapshot name for ${repositoryTitle}"
    def lastSnapshot = sh(returnStdout: true, script: "git branch -r --list 'origin/snapshot-*' --sort=-committerdate | sort --version-sort --reverse | head -1").trim()
    echo "Current latest snapshot: ${lastSnapshot}"
    def (branchPrefix, version, fabricVersion) = lastSnapshot.tokenize("-")
    def (majorVer, minorVer) = version.tokenize(".")
    int minorVersion = (minorVer as int)
    def newGitTag = "${branchPrefix}-${majorVer}.${minorVersion + 1}-${FABRIC_VERSION}"

    newTag = newGitTag.split("/")[1]
    echo "New Tag for ${repositoryTitle}: ${newTag}"
    newTag
}

def updateComposeFilesWithVersions(fabricVersion, fabricStarterTag) {
    updateImagesReferencesVersion('docker-compose.yaml', fabricVersion, fabricStarterTag)
    updateImagesReferencesVersion('docker-compose-clean.yaml', fabricVersion, fabricStarterTag)
    updateImagesReferencesVersion('docker-compose-orderer.yaml', fabricVersion, fabricStarterTag)
}

def updateImagesReferencesVersion(yamlFile, fabricVersion, fabricRestVersion) {

    fileContent = readFile yamlFile
    fileContent = fileContent.replace("\${FABRIC_VERSION:-latest}", "\${FABRIC_VERSION:-" + "${fabricVersion}" + "}")
    fileContent = fileContent.replace("\${FABRIC_STARTER_VERSION:-latest}", "\${FABRIC_STARTER_VERSION:-" + "${fabricRestVersion}" + "}")
    writeFile file: yamlFile, text: fileContent
    
    echo "Content for ${yamlFile}: ${fileContent}"

    sh "git add ${yamlFile}"
}