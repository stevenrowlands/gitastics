package org.gitistics.visitor.commit;

import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffAlgorithm.SupportedAlgorithm;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.gitistics.treewalk.TreeWalkUtils;
import org.gitistics.visitor.commit.filechange.FileChange;

public class TreeWalkVisitorRoot extends AbstractCommitVisitor {

	private DiffAlgorithm algorithm = DiffAlgorithm.getAlgorithm(SupportedAlgorithm.HISTOGRAM);

	private ObjectReader reader;

	private Repository repository;
	
	public TreeWalkVisitorRoot(Repository repository) {
		this.repository = repository;
		this.reader = repository.newObjectReader();
	}

	public void visit(RevCommit commit) {
		if (commit.getParentCount() != 0)
			return;
		
		TreeWalk walk = TreeWalkUtils.treeWalkForCommit(repository, commit);
		try {
			while (walk.next()) {
				FileChange file = new FileChange();
				file.setPath(walk.getPathString());
				file.setChangeType(ChangeType.ADD);
				file.setCommit(commit);
				callback(file);
				handleEdits(commit, walk);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			walk.release();
		}
	}

	public void handleEdits(RevCommit commit, TreeWalk walk) {
		if (!hasEditVisitors())
			return;
		
		try {
			ObjectLoader loader = reader.open(walk.getObjectId(walk.getTreeCount() - 1));

			RawText a = new RawText(loader.getBytes());
			RawText b = new RawText(new byte[0]);

			EditList edits = algorithm.diff(RawTextComparator.DEFAULT, b, a);
			edit(commit, null, edits);
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			reader.release();
		}
	}
}