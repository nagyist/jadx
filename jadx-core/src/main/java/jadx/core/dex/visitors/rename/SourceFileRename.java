package jadx.core.dex.visitors.rename;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.api.args.UseSourceNameAsClassNameAlias;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.SourceFileAttr;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.RenameReasonAttr;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.BetterName;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class SourceFileRename extends AbstractVisitor {

	@Override
	public String getName() {
		return "SourceFileRename";
	}

	@Override
	public void init(RootNode root) throws JadxException {
		final var useSourceName = root.getArgs().getUseSourceNameAsClassNameAlias();
		if (useSourceName == UseSourceNameAsClassNameAlias.NEVER) {
			return;
		}
		int repeatLimit = root.getArgs().getSourceNameRepeatLimit();
		if (repeatLimit <= 1) {
			return;
		}

		List<ClassNode> classes = root.getClasses();
		Map<String, Integer> aliasUseCount = new HashMap<>();
		for (ClassNode cls : classes) {
			aliasUseCount.put(cls.getClassInfo().getShortName(), 1);
		}
		List<ClsRename> renames = new ArrayList<>();
		for (ClassNode cls : classes) {
			if (cls.contains(AFlag.DONT_RENAME)) {
				continue;
			}
			String alias = getAliasFromSourceFile(cls);
			if (alias != null) {
				int count = aliasUseCount.merge(alias, 1, Integer::sum);
				if (count < repeatLimit) {
					renames.add(new ClsRename(cls, alias, count));
				}
			}
		}
		for (ClsRename clsRename : renames) {
			String alias = clsRename.getAlias();
			Integer count = aliasUseCount.get(alias);
			if (count < repeatLimit) {
				applyRename(clsRename.getCls(), clsRename.buildAlias(), useSourceName);
			}
		}
	}

	private static void applyRename(ClassNode cls, String alias, UseSourceNameAsClassNameAlias useSourceName) {
		if (cls.getClassInfo().hasAlias()) {
			String currentAlias = cls.getAlias();
			String betterName = getBetterName(currentAlias, alias, useSourceName);
			if (betterName.equals(currentAlias)) {
				return;
			}
		}
		cls.getClassInfo().changeShortName(alias);
		cls.addAttr(new RenameReasonAttr(cls).append("use source file name"));
	}

	private static String getBetterName(String currentName, String sourceName, UseSourceNameAsClassNameAlias useSourceName) {
		switch (useSourceName) {
			case ALWAYS:
				return sourceName;
			case IF_BETTER:
				return BetterName.getBetterClassName(sourceName, currentName);
			case NEVER:
				return currentName;
			default:
				throw new JadxRuntimeException("Unhandled strategy: " + useSourceName);
		}
	}

	private static @Nullable String getAliasFromSourceFile(ClassNode cls) {
		SourceFileAttr sourceFileAttr = cls.get(JadxAttrType.SOURCE_FILE);
		if (sourceFileAttr == null) {
			return null;
		}
		if (cls.getClassInfo().isInner()) {
			return null;
		}
		String name = sourceFileAttr.getFileName();
		name = StringUtils.removeSuffix(name, ".java");
		name = StringUtils.removeSuffix(name, ".kt");
		if (!NameMapper.isValidAndPrintable(name)) {
			return null;
		}
		if (name.equals(cls.getName())) {
			return null;
		}
		return name;
	}

	private static final class ClsRename {
		private final ClassNode cls;
		private final String alias;
		private final int suffix;

		private ClsRename(ClassNode cls, String alias, int suffix) {
			this.cls = cls;
			this.alias = alias;
			this.suffix = suffix;
		}

		public ClassNode getCls() {
			return cls;
		}

		public String getAlias() {
			return alias;
		}

		public int getSuffix() {
			return suffix;
		}

		public String buildAlias() {
			return suffix < 2 ? alias : alias + suffix;
		}

		@Override
		public String toString() {
			return "ClsRename{" + cls + " -> '" + alias + suffix + "'}";
		}
	}
}
