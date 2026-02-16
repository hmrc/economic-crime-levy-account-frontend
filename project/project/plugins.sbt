resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"
resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"

resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.jcenterRepo

addSbtPlugin("org.playframework"  % "sbt-plugin"             % "3.0.8" exclude ("ch.qos.logback", "logback-core") exclude("ch.qos.logback", "logback-classic"))

addSbtPlugin("com.timushev.sbt"   %  "sbt-updates"           % "0.6.4")