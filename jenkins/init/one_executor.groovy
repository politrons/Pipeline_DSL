def instance = jenkins.model.Jenkins.getInstance()

println "Use only one master executor"
instance.setNumExecutors(1)
