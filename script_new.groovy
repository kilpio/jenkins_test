def C_NORMAL="\033[0m"
def C_RED="\033[1;31m"
def C_GREEN="\033[1;32m"
def C_YELLOW="\033[1;33m"
def C_BLUE="\033[1;34m"
def C_MAGENTA="\033[1;35m"
def C_CYAN="\033[1;36m"
def C_WHITE="\033[1;37m"


node {
    stage('Fabric-Starter-Packages-snapshot') {
    ansiColor('xterm') {
        def newFabricStarterTag
        stage('Fabric-Starter-projects-checkout-master') {
                echo C_BLUE
    //
                stage('Fabric-Starter-checkout') {
                    checkoutFromGithubToSubfolder('fabric-starter', "${BUILD_BRANCH}")
                    dir('fabric-starter') {
                        sh "git config user.name ${GIT_REPO_OWNER}"
                        sh "git checkout master"
                        sh "git pull"
                        sh "git checkout stable"
                        sh "git pull"
                        newFabricStarterTag = evaluateNextSnapshotGitTag('Fabric-starter')
                    }
                }
                echo C_NORMAL
        } //Fabric-Starter-projects-Snapshot-master
    //
        stage ('Fabric-Starter-Merge-stable-from-master') {
                echo C_MAGENTA
                dir('fabric-starter') {

                //sh "git checkout stable || git checkout -b stable "
                 sh "git checkout -B stable"
                 sh "git status"
                    if (MERGE_FROM_MASTER == 'true') {
                        echo C_RED
                        sh "cat .env"
                        echo C_MAGENTA
                        sh "git merge --strategy-option=theirs master -m 'merge master into stable'"
                        sh "git checkout master -- ."
                        echo C_RED
                        sh "cat .env"
                        //sh "git commit -m 'stable'"
                        //sh "git commit -m 'Stable branch along with ${newFabricStarterTag}' || true"
                    }
                //
                commitAndPushToBranch('stable')
                }
                echo C_NORMAL
        }//Fabric-Starter-Merge-from-master
        
        stage ('Fabric-Starter-Merge-snapshot-from-stable') {
                echo C_CYAN
                dir('fabric-starter') {
                sh "git checkout -B ${newFabricStarterTag}" 
                    if (MERGE_FROM_MASTER == 'true') {
                        sh "git merge --strategy-option=theirs stable -m 'merge stable into ${newFabricStarterTag}'"
                        sh "git checkout master -- ."
                        echo C_RED
                        sh "cat .env"
                        echo C_CYAN
                        //sh "git commit -m 'Snapshot branch ${newFabricStarterTag} from current stable' || true"
                    }
                commitAndPushToBranch(newFabricStarterTag)
                }
                echo C_NORMAL
        }//Fabric-Starter-Merge-from-master
    //
        stage('Fabric-tools-extended-image') {
                echo C_YELLOW
                dir('fabric-starter') {
                    buildAndPushDockerImage("fabric-tools-extended", newFabricStarterTag, "--no-cache --build-arg=FABRIC_VERSION=${FABRIC_VERSION} -f fabric-tools-extended/Dockerfile .")
                }
                echo C_NORMAL
        } //Fabric-tools-extended
    //
        // // stage('Fabric-Starter-snapshot') {
        // //     echo C_WHITE
        // //         dir('fabric-starter') {
        // //             commitAndPushToBranch(newFabricStarterTag)
        // //             commitAndPushToBranch(BUILD_BRANCH)
        // //         }
        // //     echo C_NORMAL
        // // }//Fabric-Starter-snapshot
    //
    //! Fabric-starter-rest stuff
        stage('Fabric-Starter-rest-chec
        kout') {
                echo C_CYAN
                checkoutFromGithubToSubfolder('fabric-starter-rest')
                echo C_NORMAL
        } //Fabric-Starter-rest-checkout
    //
        stage('Fabric-Starter-rest-image') {
                echo C_GREEN
                dir('fabric-starter-rest') {
                    buildAndPushDockerImage('fabric-starter-rest', newFabricStarterTag, "--no-cache -f Dockerfile .")
                }
                echo C_NORMAL
        } //Fabric-Starter-rest-image
    //
        stage('Fabric-Starter-Rest-snapshot') {
            echo C_BLUE
                dir('fabric-starter-rest') {
                    snapshotFabricStarterRest(newFabricStarterTag)
                    snapshotFabricStarterRest(BUILD_BRANCH)
                }
            echo C_NORMAL
        }//Fabric-Starter-snapshot
    } //Fabric-Starter-Packages-snapshot
    } //AnsiColor
}//node
//! ===================================================================================
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
                        //sh "git checkout -B ${branchName}"
                            //sh "git status"
                        sh "git config user.name ${GIT_REPO_OWNER}"


                        
                        sh "git checkout ${branchName}"
                        sh "git status"
                        printRed "----------------------------------------"
                        sh "cat .env"
                        

                            //sh "git status"
                        def fileContent = readFile '.env'
                        writeFile file: '.env', text: "${fileContent}\nFABRIC_STARTER_VERSION=${branchName}\nFABRIC_VERSION=${FABRIC_VERSION}"
                        sh "git add .env"

                        printRed "----------------------------------------"
                        sh "cat .env"
                        

                        updateComposeFilesWithVersions(FABRIC_VERSION, branchName)
                        sh "git status"
                        
                        sh "git commit -m '${branchName}' || true" 
                        sh "git status"
                        sh("git push -u origin ${branchName}")
                            //sh "git status"    
                        //sh "git checkout ${BUILD_BRANCH}"
                            //sh "git status"
                        //sh "git push"
                    }
                }

private void snapshotFabricStarterRest(snapshotName) {
       sshagent(credentials: ["${GITHUB_SSH_CREDENTIALS_ID}"]) {
                        //sh "git status"
                    sh "git checkout -B ${snapshotName}"
                        //sh "git status"
                    sh("git push -u origin ${snapshotName}")
                        //sh "git status"
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