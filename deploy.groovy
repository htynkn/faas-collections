#!/usr/bin/env groovy

@Grapes([
        @Grab(group = 'org.apache.httpcomponents', module = 'fluent-hc', version = '4.4.1'),
        @Grab(group = 'com.alibaba', module = 'fastjson', version = '1.2.56'),
        @Grab(group = 'org.eclipse.jgit', module = 'org.eclipse.jgit', version = '5.2.1.201812262042-r')
])

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.fluent.Request;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

String buildsString = Request.Get("https://api.travis-ci.org/repos/htynkn/faas-collections/builds")
        .addHeader("Accept", "application/vnd.travis-ci.2.1+json").execute().returnContent().asString();

JSONObject jsonObject = JSON.parseObject(buildsString);


JSONArray builds = (JSONArray) jsonObject.get("builds");

JSONObject successBuild = builds.toJavaList(JSONObject.class).stream().filter(new Predicate<JSONObject>() {
    @Override
    public boolean test(JSONObject o) {
        return "passed".equalsIgnoreCase(o.getString("state"));
    }
}).findFirst().orElseThrow(new Supplier<Throwable>() {
    @Override
    public Throwable get() {
        return new RuntimeException("No success builds");
    }
});

String commitId = successBuild.getString("commit_id");

System.out.println("Find latest success commit: " + commitId);

JSONArray commits = (JSONArray) jsonObject.get("commits");

JSONObject commitInfo = commits.toJavaList(JSONObject.class).stream().filter(new Predicate<JSONObject>() {
    @Override
    public boolean test(JSONObject o) {
        return commitId.equalsIgnoreCase(o.getString("id"));
    }
}).findFirst().orElseThrow(new Supplier<Throwable>() {
    @Override
    public Throwable get() {
        return new RuntimeException("No related commit");
    }
});

String successSha = commitInfo.getString("sha");

System.out.println("Find latest success sha: " + successSha);


String basePath = "./";

File baseFolder = new File(basePath);
List<String> folderNames = Arrays.stream(baseFolder.listFiles(new FileFilter() {
    @Override
    public boolean accept(File pathname) {
        return pathname.isDirectory() && !pathname.getName().startsWith(".");
    }
})).map(new Function<File, String>() {
    @Override
    public String apply(File file) {
        return file.getName();
    }
}).collect(Collectors.toList());

System.out.println("Find target folders :");

for (String targetFolder : folderNames) {
    System.out.println("folder: " + targetFolder);
}

Set<String> foldersNeedDeploy = new HashSet<>();

Repository repo = FileRepositoryBuilder.create(new File(basePath + ".git"));
try {
    Git git = new Git(repo);
    try {
        ObjectReader reader = repo.newObjectReader();
        try {
            ObjectId newObjectId = repo.resolve("HEAD^{tree}");
            ObjectId oldObjectId = repo.resolve(successSha + "^{tree}");


            System.out.println("Starting diff " + oldObjectId + " : " + newObjectId);

            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            oldTreeIter.reset(reader, oldObjectId);
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
            newTreeIter.reset(reader, newObjectId);

            List<DiffEntry> diffs = git.diff().setNewTree(newTreeIter).setOldTree(oldTreeIter).call();

            for (DiffEntry entry : diffs) {
                System.out.println("Entry: " + entry);

                for (String folderName : folderNames) {
                    if (entry.getNewPath().contains(folderName) || entry.getOldPath().contains(folderName)) {
                        System.out.println("Need deploy folder: " + folderName);
                        foldersNeedDeploy.add(folderName);
                    }
                }

            }

            for (String folderNeedDeploy : foldersNeedDeploy) {
                System.out.println("Start deploy folder: " + folderNeedDeploy);

                ProcessBuilder pb = new ProcessBuilder("./deploy.sh " + folderNeedDeploy);
                pb.inheritIO();
                Process p = pb.start();

                String line;

                BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while ((line = stdoutReader.readLine()) != null) {
                    System.out.println(" .. stdout: " + line);
                }
                BufferedReader stderrReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                while ((line = stderrReader.readLine()) != null) {
                    System.err.println(" .. stderr: " + line);
                }

                int result = p.waitFor();
                if (result == 0) {
                    System.out.println("Deploy success for " + folderNeedDeploy);
                }
            }
        } finally {
            reader.close();
        }
    } finally {
        git.close();
    }
} finally {
    repo.close();
}