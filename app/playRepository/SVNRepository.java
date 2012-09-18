package playRepository;

import java.io.*;
import java.util.*;

import javax.servlet.*;

import org.codehaus.jackson.node.ObjectNode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.tigris.subversion.javahl.*;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import play.libs.Json;

import utils.FileUtil;


public class SVNRepository implements PlayRepository {

    public static final String REPO_PREFIX = "repo/svn/";

    private String projectName;

    private String userName;

    public SVNRepository(final String userName, String projectName) throws ServletException {
        this.userName = userName;
        this.projectName = projectName;
    }


    public byte[] getRawFile(String path) throws SVNException {
        SVNURL svnURL = SVNURL.fromFile(new File(REPO_PREFIX + userName + "/" + projectName));
        org.tmatesoft.svn.core.io.SVNRepository repository = SVNRepositoryFactory.create(svnURL);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        repository.getFile(path, -1l, null, baos);
        return baos.toByteArray();
    }

    public ObjectNode findFileInfo(String path) throws SVNException {
        SVNURL svnURL = SVNURL.fromFile(new File(REPO_PREFIX + userName + "/" + projectName));
        org.tmatesoft.svn.core.io.SVNRepository repository = SVNRepositoryFactory.create(svnURL);
        
        SVNNodeKind nodeKind = repository.checkPath(path , -1 );
        
        if(nodeKind == SVNNodeKind.DIR){
            //폴더 내용 출력
            ObjectNode result = Json.newObject();
            SVNProperties prop = new SVNProperties();
            
            Collection entries = repository.getDir(path, -1, prop, (Collection)null);
            
            Iterator iterator = entries.iterator( );
            while ( iterator.hasNext( ) ) {
                SVNDirEntry entry = ( SVNDirEntry ) iterator.next( );
                
                ObjectNode data = Json.newObject();
                data.put("type", entry.getKind() == SVNNodeKind.DIR ? "folder" : "file");
                data.put("commitMessage", entry.getCommitMessage());
                data.put("commiter", entry.getAuthor());
                data.put("commitDate", entry.getDate().toString());
                
                result.put(entry.getName(), data);
            }
            
            return result;
            
        } else if(nodeKind == SVNNodeKind.FILE) {
            //파일 내용 출력
            ObjectNode result = Json.newObject();
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            SVNProperties prop = new SVNProperties();
            repository.getFile(path, -1l, prop, baos);
            
            result.put("commitMessage", prop.getStringValue(SVNProperty.COMMITTED_REVISION));
            result.put("commiter", prop.getStringValue(SVNProperty.LAST_AUTHOR));

            result.put("commitDate", prop.getStringValue(SVNProperty.COMMITTED_DATE));

            result.put("data", baos.toString());
            return result;
        } else {
            return null;
        }
    }

    @Override
    public void create() throws ClientException {
        String svnPath = new File(SVNRepository.REPO_PREFIX + userName + "/" + projectName)
                .getAbsolutePath();
        new org.tigris.subversion.javahl.SVNAdmin().create(svnPath, false, false, null, "fsfs");
    }

    @Override
    public void delete() {
        FileUtil.rm_rf(new File(REPO_PREFIX + userName + "/" + projectName));
    }

    @Override
    public String getPatch(String commitId) throws SVNException {
        // Prepare required arguments.
        SVNURL svnURL = SVNURL.fromFile(new File(REPO_PREFIX + userName + "/" + projectName));
        long rev = Integer.parseInt(commitId);

        // Get diffClient.
        SVNClientManager clientManager = SVNClientManager.newInstance();
        SVNDiffClient diffClient = clientManager.getDiffClient();

        // Using diffClient, write the changes by commitId into
        // byteArrayOutputStream, as unified format.
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        diffClient.doDiff(svnURL, null, SVNRevision.create(rev), SVNRevision.create(rev - 1),
                SVNDepth.INFINITY, true, byteArrayOutputStream);

        return byteArrayOutputStream.toString();
    }

    @Override
    public List<Commit> getHistory(int page, int limit) throws AmbiguousObjectException,
            IOException, NoHeadException, GitAPIException, SVNException {
        // Get the repository
        SVNURL svnURL = SVNURL.fromFile(new File(REPO_PREFIX + userName + "/" + projectName));
        org.tmatesoft.svn.core.io.SVNRepository repository = SVNRepositoryFactory.create(svnURL);

        // path to get log
        String[] paths = {"/"};

        // Determine revisions
        long startRevision = repository.getLatestRevision();
        long endRevision = startRevision - limit;
        if (endRevision < 1) {
            endRevision = 1;
        }

        // Get the logs
        List<Commit> result = new ArrayList<Commit>();
        for(Object entry : repository.log(paths, null, startRevision, endRevision, false, false)) {
            result.add(new SvnCommit((SVNLogEntry) entry));
        }

        return result;
    }

}
