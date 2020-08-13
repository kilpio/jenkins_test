def C_NORMAL="\033[0m"
def C_RED="\033[1;31m"
def C_GREEN="\033[1;32m"
def C_YELLOW="\033[1;33m"
def C_BLUE="\033[1;34m"
def C_MAGENTA="\033[1;35m"
def C_CYAN="\033[1;36m"
def C_WHITE="\033[1;37m"
def C_FRAMED="\033[51m"
def C_NOTFRAMED="\033[54m"
def C_UNDERLINED="\033[4m"
def C_NOTUNDERLINED="\033[24m"
//https://en.wikipedia.org/wiki/ANSI_escape_code

node {
    stage('Fabric-Starter-snapshot') {
        ansiColor('xterm') {
            def newFabricStarterTag
            echo C_RED
            stage('Fabric-Starter-git-checkout-pull-master') {
                echo C_BLUE
                checkoutFromGithubToSubfolder('fabric-starter', "${BUILD_BRANCH}")
                dir('fabric-starter') {
                    sh "git checkout master"
                    sh "git pull"
                    newFabricStarterTag = evaluateNextSnapshotGitTag('Fabric-starter')
                }
                echo C_NORMAL                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               
            }

            echo C_RED
            stage ('Fabric-Starter-Merge-master-to-stable') {
                echo C_MAGENTA
                dir('fabric-starter') {
                    updateAndCommitBranch('master','latest','stable')
                }
                echo C_NORMAL
            }

            echo C_RED
            stage ('Fabric-Starter-Merge-stable-to-snapshot') {
                echo C_CYAN                                                                                                                                                                                         
                dir('fabric-starter') {
                    updateAndCommitBranch('stable','stable',newFabricStarterTag) 
                }
                echo C_NORMAL
            }

            echo C_RED
            stage('Fabric-Tools-Extended-build-extended-images') {
                echo C_YELLOW
                dir('fabric-starter') {
                   
                    buildDockerImage("fabric-tools-extended", newFabricStarterTag, newFabricStarterTag, "--no-cache --build-arg=FABRIC_VERSION=${FABRIC_VERSION} -f fabric-tools-extended/Dockerfile .")
                    buildDockerImage("fabric-tools-extended", 'stable', 'stable', "--no-cache --build-arg=FABRIC_VERSION=${FABRIC_VERSION} -f fabric-tools-extended/Dockerfile .")
                    buildDockerImage("fabric-tools-extended", 'latest', 'master', "--no-cache --build-arg=FABRIC_VERSION=${FABRIC_VERSION} -f fabric-tools-extended/Dockerfile .")
                   
                }
                echo C_NORMAL
            }
            
            echo C_RED
            stage('Farbric-starter-REST-checkout') {
                echo C_CYAN
                checkoutFromGithubToSubfolder('fabric-starter-rest')
                dir('fabric-starter-rest') {
                    sh "git checkout master"
                    sh "git pull"
                }    
                echo C_NORMAL
            }

            echo C_RED
            stage('Farbric-starter-REST-merge-master-to-stable') {
                echo C_BLUE
                    dir('fabric-starter-rest') {
                        updateAndCommitRESTBranch('master','latest','stable')
                    }
                echo C_NORMAL
            }

            echo C_RED
            stage('Farbric-starter-REST-copy-stable-to-snapshot') {
                echo C_YELLOW
                dir('fabric-starter-rest') {
                        updateAndCommitRESTBranch('stable','stable',newFabricStarterTag) 
                    }
                echo C_NORMAL
            }

            echo C_RED
            stage('Fabic-Starter-REST-build-docker-images') {
                echo C_GREEN
                dir('fabric-starter-rest') {
                  
                    buildDockerImage('fabric-starter-rest', newFabricStarterTag, newFabricStarterTag, "--no-cache -f Dockerfile .")
                    buildDockerImage('fabric-starter-rest', 'stable', 'stable', "--no-cache -f Dockerfile .")
                    buildDockerImage('fabric-starter-rest', 'latest', 'master', "--no-cache -f Dockerfile .")
                   
                }
                echo C_NORMAL
            }

//? ==================================================== DOCKER ================================================

//kilpio/fabric-starter-rest                          latest              
//kilpio/fabric-starter-rest                          stable              
//kilpio/fabric-starter-rest                          snapshot-0.66-1.4.6 
//kilpio/fabric-tools-extended                        latest              
//kilpio/fabric-tools-extended                        stable              
//kilpio/fabric-tools-extended                        snapshot-0.66-1.4.6 


            echo C_RED
            stage('Fabic-Starter-REST-push-docker-images') {
                echo C_BLUE
                
                pushDockerImage('fabric-starter-rest', newFabricStarterTag)
                pushDockerImage('fabric-starter-rest', 'stable')
                pushDockerImage('fabric-starter-rest', 'latest')
                echo C_NORMAL
            }


            echo C_RED
            stage('Fabic-Starter-push-docker-images') {
                echo C_CYAN
                pushDockerImage("fabric-tools-extended", newFabricStarterTag)
                pushDockerImage("fabric-tools-extended", 'stable')
                pushDockerImage("fabric-tools-extended", 'latest')

                
                echo C_NORMAL
            }

//? ========================================================= GIT =================================================
            echo C_RED
            stage('Fabic-Starter-REST-git-push-snapshot') {
                echo C_BLUE
                dir('fabric-starter-rest') {
                        gitPushToBranch(newFabricStarterTag)
                }
                echo C_NORMAL
            }

            echo C_RED
            stage('Fabic-Starter-REST-git-push-stable') {
                echo C_GREEN
                dir('fabric-starter-rest') {
                    gitPushToBranch('stable')
                }   
                echo C_NORMAL
            }

            echo C_RED
            stage ('Fabic-Starter-git-push-snapshot') {
                echo C_MAGENTA
                dir('fabric-starter') {
                    gitPushToBranch(newFabricStarterTag)
                }
                echo C_NORMAL
            }

            echo C_RED
            stage ('Fabic-Starter-git-push-stable') {
                echo C_BLUE
                dir('fabric-starter') {
                gitPushToBranch('stable')
                }
                echo C_NORMAL
            }    

        } //AnsiColor
    } //Fabric-Starter-Packages-snapshot
}//node

//! ===================================================================================

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


void buildDockerImage(imageName, tag, branchToBuildImageFrom, def args = '') {
    
    def C_UNDERLINED="\033[4m"
    def C_NOTUNDERLINED="\033[24m"
    def C_RED="\033[1;31m"

    echo C_UNDERLINED
    echo imageName
    echo tag
    echo branchToBuildImageFrom

    sh "docker image ls"
    echo C_NOTUNDERLINED

    sh "git checkout ${branchToBuildImageFrom}"
    if (!args?.trim()) {
        args = "-f Dockerfile ."
    }

    args = "-t ${DOCKER_REPO}/${imageName}:${tag} ${args}"
    echo "docker build args: $args"

    
    sh "docker image build ${args}"
    
    echo C_UNDERLINED
    sh "docker image ls"
    echo C_RED
    echo imageName
    echo tag
    echo branchToBuildImageFrom

    echo C_NOTUNDERLINED
}


private void pushDockerImage(imageName, tag) {

    docker.withRegistry("${DOCKER_REGISTRY}", "${DOCKER_CREDENTIALS_ID}") {

        fabricRestImage = docker.image("${DOCKER_REPO}/${imageName}:${tag}")
        fabricRestImage.push()
        //fabricRestImage.push('latest')
        //fabricRestImage.push('stable')
    }
}


void commitBranch(branchName) {
                sh "git checkout ${branchName}"
                sh "git commit -m '${branchName}' || true"
}

private void gitPushToBranch(branchName) {
        sshagent(credentials: ["${GITHUB_SSH_CREDENTIALS_ID}"]) {
        sh "git config user.name ${GIT_REPO_OWNER}"
        sh "git checkout ${branchName}"
        sh("git push -u origin ${branchName}")
    }
}
                            // master:      latest   ->   stable
                            // stable:      stable   ->   snapshot
void updateAndCommitBranch(fromBranchName, replaceTag, toBranchName) {
    checkoutAndThenPullIfRemoteExists(toBranchName)       
    if (MERGE_FROM_MASTER == 'true') {
        sh "git merge --strategy-option=theirs ${fromBranchName} -m \"merge ${fromBranchName} into ${toBranchName}\""
        sh "git checkout ${fromBranchName} -- ."

        sh "git checkout master -- .env"
        envAppendVersionVars(toBranchName,FABRIC_VERSION)
    }  

    updateEnvFileWithVersions(FABRIC_VERSION, replaceTag, toBranchName)
    updateComposeFilesWithVersions(FABRIC_VERSION, replaceTag, toBranchName)

    commitBranch(toBranchName)
}


void updateAndCommitRESTBranch(fromBranchName, replaceTag, toBranchName) {
    checkoutAndThenPullIfRemoteExists(toBranchName)       
    if (MERGE_FROM_MASTER == 'true') {
        sh "git merge --strategy-option=theirs ${fromBranchName} -m \"merge ${fromBranchName} into ${toBranchName}\""
        sh "git checkout ${fromBranchName} -- ."
    }  
    commitBranch(toBranchName)
}


void checkoutAndThenPullIfRemoteExists(toBranchName) {
    if (remoteBranchExists(toBranchName)) {
        sh "git checkout ${toBranchName}"
        sh "git pull"
    } else {
        sh "git checkout -B ${toBranchName}"    
    }
}


void envAppendVersionVars(currentBranchName, fabricVersion) {
    def fileContent = readFile '.env'
    writeFile file: '.env', text: "${fileContent}\nFABRIC_STARTER_VERSION=${currentBranchName}\nFABRIC_VERSION=${fabricVersion}"
    sh "git add .env"    
}

                                // master:      latest   ->   stable
                                // stable:      stable   ->   snapshot
void updateEnvFileWithVersions(fabricVersion, replaceTag, currentBranch) {
    updateImagesReferencesVersion('.env', fabricVersion, replaceTag, currentBranch)
}


def updateComposeFilesWithVersions(fabricVersion, replaceTag, currentBranch) { //mergeBranch -> currenBranch
    updateImagesReferencesVersion('docker-compose.yaml', fabricVersion, replaceTag, currentBranch)
    updateImagesReferencesVersion('docker-compose-clean.yaml', fabricVersion, replaceTag, currentBranch)
    updateImagesReferencesVersion('docker-compose-orderer.yaml', fabricVersion, replaceTag, currentBranch)
    updateImagesReferencesVersion('docker-compose-ssl.yaml', fabricVersion, replaceTag, currentBranch)
}

def updateImagesReferencesVersion(fileToProcess, fabricVersion, replaceTag, currentBranch) {
    fileContent = readFile fileToProcess
    
    fileContent = fileContent.replace("\${FABRIC_VERSION:-"+"${replaceTag}"+"}", "\${FABRIC_VERSION:-" + "${fabricVersion}" + "}")
    fileContent = fileContent.replace("\${FABRIC_STARTER_VERSION:-"+"${replaceTag}"+"}", "\${FABRIC_STARTER_VERSION:-" + "${currentBranch}" + "}")
    
    fileContent = fileContent.replace("FABRIC_VERSION="+"${replaceTag}", "FABRIC_VERSION=" + "${fabricVersion}")
    fileContent = fileContent.replace("FABRIC_STARTER_VERSION="+"${replaceTag}", "FABRIC_STARTER_VERSION=" + "${currentBranch}")

    writeFile file: fileToProcess, text: fileContent

    echo "Content for ${fileToProcess}: ${fileContent}"

    sh "git add ${fileToProcess}"
}


// private void snapshotFabricStarterRest(snapshotName) {
//     sshagent(credentials: ["${GITHUB_SSH_CREDENTIALS_ID}"]) {
//                     sh("git push -u origin ${snapshotName}")
//                 }
// }


def remoteBranchExists(branchName) {
    def checkRemoteBranch=sh(script: "git branch --list -r origin/${branchName} | wc -l", returnStdout: true).toString().trim()
    def whetherExists=checkRemoteBranch.isInteger() ? checkRemoteBranch.toInteger() : 0
    //if (whetherExists > 0) {return true} else {return false}
    return (whetherExists > 0)
}


//! ===========================================================================================================================================================

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