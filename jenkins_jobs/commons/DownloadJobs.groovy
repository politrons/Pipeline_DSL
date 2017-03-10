import groovyx.net.http.HTTPBuilder
import groovy.json.*

class DownloadJobsFromBranches {

//    github.com/
    String apiSdkRepo = "repos/politrons/API_SDK"
    File branchesFolder = new File("downloaded_jobs")
    String git_api = "https://github.com/api/v3/"
    HTTPBuilder http = new HTTPBuilder(git_api)

    static void getBrachesNames() {
        System.properties.with {
            assert USERNAME != null
            assert PASSWORD != null
        }

        new DownloadJobsFromBranches().with {
            setBasicAuthentication(System.properties.USERNAME, System.properties.PASSWORD)
            downloadScripts()
        }
    }

    def setBasicAuthentication(String user, String password) {
        def auth = "$user:$password".bytes.encodeBase64().toString()
        http.setHeaders(Authorization: "Basic $auth")
    }

    def downloadScripts() {
//        clearScriptsFolder()

        get("$apiSdkRepo/branches")
            .each this.&downloadAndSave
    }

//    def clearScriptsFolder() {
//        if (branchesFolder.exists()) branchesFolder.deleteDir()
//        branchesFolder.mkdir()
//    }

    def downloadAndSave(branch) {
//
//        def fileTree = get("$price/git/trees/${branch.commit.sha}").tree
//        def jobsFolder = fileTree.find { it.path == "jenkins_jobs" }
//
//        if (jobsFolder) {
            println "Found jobs in $branch.name"

//            def branchFolder = new File(branchesFolder, branch.name)
//            branchFolder.mkdir()
//
//            save(branch, branchFolder, download(jobsFolder.url))
//        }
    }

//    /**
//     * Return a recursive map that contains name of file/folder and it's content,
//     * which is a map or a string.
//     */
//    Map<String, Object> download(String treeUrl) {
//        def map = [:]
//        get(treeUrl).tree.each { fileList ->
//            if (fileList.type == "tree") map[fileList.path] = download(fileList.url)
//            else if (fileList.type == "blob") map[fileList.path] = contentToString(get(fileList.url))
//            else throw new RuntimeException("not handled yet")
//        }
//
//        return map
//    }
//
//    String contentToString(jsonResponse){
//        new String(jsonResponse.content.decodeBase64())
//    }
//
//    /**
//     * Recursively save content of map to given folder.
//     */
//    void save(branch, File folder, Map map) {
//        if (!folder.exists()) folder.mkdir()
//
//        map.each { name, content ->
//            def currentFile = new File(folder, name)
//            println "created $currentFile"
//
//            if (content instanceof String) {
//                saveFile(branch, currentFile, content)
//            } else {
//                save(branch, currentFile, content)
//            }
//        }
//    }
//
//    /**
//     * I need to provide information about branch the script is running on, so it is easiest to
//     * just prepend some properties to beginning of the files that are scripts.
//     * Because of various reasons I can't prepend it to "classes".
//     */
//    void saveFile(branch, file, fileText) {
//        def isTopLevelScript = file.name.endsWith(".groovy") && !fileText.startsWith("package ")
//
//        if (isTopLevelScript) {
//            file << """
//                String branch_name = '$branch.name'
//                String branch_hash = '$branch.commit.sha'
//                boolean is_master = ${branch.name.toString() == 'master'}
//            """.stripIndent().trim()
//
//            file << "\n"
//        }
//
//        file << fileText
//    }

    /**
     * If response returns json it will be parsed and returned as map.
     */
    def get(String path) {
        http.get(path: path)
    }

    def p(map) {
        println JsonOutput.prettyPrint(JsonOutput.toJson(map).toString())
    }
}
