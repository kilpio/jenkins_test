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
def C_NORMAL="\033[0m"
def C_RED="\033[1;31m"
def C_GREEN="\033[1;32m"
def C_YELLOW="\033[1;33m"
def C_BLUE="\033[1;34m"
def C_MAGENTA="\033[1;35m"
def C_CYAN="\033[1;36m"
def C_WHITE="\033[1;37m"





node {

    stage('Fabric-Starter-projects-Snapshot') {

        def newFabricStarterTag
        
        ansiColor('xterm') { 
        echo C_BLUE

        stage('Fabric-tools-extended') {

            //! Wiping out workspace first.
            //! Cloning the remote Git repository
            //? git config remote.origin.url git@github.com:kilpio/fabric-starter.git 

            checkoutFromGithubToSubfolder('fabric-starter', "${BUILD_BRANCH}")
            // sh("cp -r build/chaincode/node/dns fabric-starter/chaincode/node")
            
                //! Running in /var/jenkins_home/workspace/test/fabric-starter
                //! HEAD detached at xxxxxx
            dir('fabric-starter') {
                //sh "git status"                
            sh "git config user.name ${GIT_REPO_OWNER}" //? kilpio
                //sh "git status"
            sh "git checkout master"
                //sh "git status"
                //! Switched to a new branch 'master'
                //! Branch master set up to track remote branch master from origin.
                //! Your branch is up-to-date with 'origin/master'.
            sh "git pull"
                //! Already up-to-date.
                //sh "git status"
                //! nothing to commit, working tree clean
            sh "git checkout ${BUILD_BRANCH}"
                //! Switched to a new branch 'stable'
                //! Branch stable set up to track remote branch stable from origin.
                //sh "git status"
            sh "git pull"
                //sh "git status"
                //! On branch stable
                //! Your branch is up-to-date with 'origin/stable'.
                //! nothing to commit, working tree clean

                //? git branch -r --list origin/snapshot-* --sort=-committerdate
                //? + sort --version-sort --reverse
                //? + head -1

                newFabricStarterTag = evaluateNextSnapshotGitTag('Fabric-starter')

                if (AUTO_MERGE_FROM_MASTER == 'true') {
                    print"-- AUTO_MERGE_FROM_MASTER"
                        //sh "git status"
                        //! On branch stable
                        //! Your branch is up-to-date with 'origin/stable'
                    sh "git checkout master -- ."
                        //! On branch stable
                        //! Your branch is up-to-date with 'origin/stable'.
                        //! Changes to be committed:
                        //! (use "git reset HEAD <file>..." to unstage)

                        //!     modified:   .env
                        //!     modified:   docker-compose-clean.yaml
                        //!     modified:   docker-compose-orderer.yaml
                        //!     modified:   docker-compose.yaml
                       
                        //sh "git status"
                    //? git commit -m Stable branch along with snapshot-0.xx-1.4.x
                    sh "git commit -m 'Stable branch along with ${newFabricStarterTag}' || true"
                    //! Your branch is ahead of 'origin/stable' by 1 commit.
                        sh "git status"
                }


              }
                // buildAndPushDockerImage("fabric-tools-extended", newFabricStarterTag, "--no-cache --build-arg=FABRIC_VERSION=${FABRIC_VERSION} -f fabric-tools-extended/Dockerfile .")
            }
            echo C_NORMAL
        }

        stage("Build Stable docker image of fabric-starter-rest") {
            ansiColor('xterm') { 
        
            echo C_CYAN
            //? Cloning repository git@github.com:kilpio/fabric-starter-rest.git
            checkoutFromGithubToSubfolder('fabric-starter-rest')

            dir('fabric-starter-rest') {
                //buildAndPushDockerImage('fabric-starter-rest', newFabricStarterTag, "--no-cache -f Dockerfile .")
                }
            echo C_NORMAL
            }
        }

        stage('Fabric-Starter') {
            ansiColor('xterm') {
            echo C_MAGENTA
            //! SNAPSHOT FABRIC-STARTER
            //printRed 'SNAPSHOT FABRIC-STARTER'
            stage('Snapshot fabric-starter') {
//                checkoutFromGithubToSubfolder('fabric-starter')

                dir('fabric-starter') {

                            commitAndPushToBranch(newFabricStarterTag)

                            //!
                            //? git checkout -B snapshot-0.xx-1.4.x
                            //! Switched to a new branch 'snapshot-0.xx-1.4.x'
                            //! On branch snapshot-0.xx-1.4.x
                            //? git add .env
                            //? git add docker-compose.yaml
                            //? git add docker-compose-clean.yaml
                            //? git add docker-compose-orderer.yaml
                            //? git add 

                            //! On branch snapshot-0.xx-1.4.x
                            //! Changes to be committed:
                            //! modified:   .env

                            //! git push -u origin snapshot-0.xx-1.4.x

                            //! * [new branch]      snapshot-0.39-1.4.6 -> snapshot-0.39-1.4.6
                            //! Branch snapshot-0.39-1.4.6 set up to track remote branch snapshot-0.39-1.4.6 from origin.

                            commitAndPushToBranch(BUILD_BRANCH)
                            //! origin/stable
                }
            }
            echo C_NORMAL
            }
        }

        stage('Snapshot fabric-starter-rest') {
            //! SNAPSHOT FABRIC-STARTER-REST
            //ansiColor('xterm') {
            //   echo "\033[51mSNAPSHOT FABRIC-STARTER-REST\033[0m"
            //}
            ansiColor('xterm') {
            echo C_GREEN
            dir('fabric-starter-rest') {
            
                snapshotFabricStarterRest(newFabricStarterTag)
                snapshotFabricStarterRest(BUILD_BRANCH)

            }
            echo C_NORMAL
            }
        }
    }
}

//? ------------------------------------------------------------------------------------------------------

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
        fabricRestImage.push('stable')
    }
}


private void commitAndPushToBranch(branchName) {
                    sshagent(credentials: ["${GITHUB_SSH_CREDENTIALS_ID}"]) {
                            //sh "git status"
                        sh "git checkout -B ${branchName}"
                            //sh "git status"
                        sh "git c
                        onfig user.name ${GIT_REPO_OWNER}"
                            //sh "git status"
                        def fileContent = readFile '.env'
                        writeFile file: '.env', text: "${fileContent}\nFABRIC_STARTER_VERSION=${branchName}\nFABRIC_VERSION=${FABRIC_VERSION}"
                        sh "git add .env"

                        updateComposeFilesWithVersions(FABRIC_VERSION, branchName)
                            //sh "git status"
                        sh "git commit -m '${branchName}'"
                            //sh "git status"
                        sh("git push -u origin ${branchName}")
                            //sh "git status"    
                        sh "git checkout ${BUILD_BRANCH}"
                            //sh "git status"
                        sh "git push"
                    }
                }

private void snapshotFabricStarterRest(snapshotName) {
       sshagent(credentials: ["${GITHUB_SSH_CREDENTIALS_ID}"]) {
                        sh "git status"
                    sh "git checkout -B ${snapshotName}"
                        sh "git status"
                    sh("git push -u origin ${snapshotName}")
                        sh "git status"
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

def printRed(message) {
    ansiColor('xterm') {
        echo "\033[1;31m${message}\033[0m"
    }
}

def printGreen(message) {
    ansiColor('xterm') {
        echo "\033[1;32m${message}\033[0m"
    }
}

def printBlue(message) {
    ansiColor('xterm') {
        echo "\033[1;34m${message}\033[0m"
    }
}

def printMagenta(message) {
    ansiColor('xterm') {
        echo "\033[1;32m${message}\033[0m"
    }
}

