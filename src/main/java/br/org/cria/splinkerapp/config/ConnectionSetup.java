package br.org.cria.splinkerapp.config;

import com.github.fracpete.processoutput4j.output.ConsoleOutputProcessOutput;
import com.github.fracpete.rsync4j.Ssh;
import com.github.fracpete.rsync4j.SshKeyGen;

public class ConnectionSetup {

    public static void createKeys()
    {
        try
        {
            var path = "%s/spLinker.key".formatted(System.getProperty("user.dir"));
            SshKeyGen keygen = new SshKeyGen()
                    .outputCommandline(true)
                    .verbose(10)
                    .keyType("rsa")
                    .newPassPhrase("")
                    //.comment("test key")
                    .keyFile(path);


            var output = new ConsoleOutputProcessOutput();
            output.monitor(keygen.builder());
                    }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }

    }

    public static void firstConnection()
    {
        try
        {
            Ssh ssh = new Ssh()
                    .outputCommandline(true)
                    .option("StrictHostKeyChecking=no")
                    .identifyFile("~/.ssh/spLinker")
                    .verbose(1)
                    .hostname("bruno@35.224.172.146")
                    .command("python -c \"print('hello')\"");
            ConsoleOutputProcessOutput output = new ConsoleOutputProcessOutput();
            output.monitor(ssh.builder());
            var success = output.hasSucceeded();
            var timedOut = output.hasTimedOut();
            var exitCode = output.getExitCode();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
