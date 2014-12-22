package net.moznion.mysql.diff;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;

@BuildParseTree
public class TableDefinitionParser extends BaseParser<Object> {
	// Entry point
	public Rule Statements() {
		return ZeroOrMore(Statement());
	}

	public Rule Statement() {
		return FirstOf(
				Comment(),
				Use(),
				Set(),
				Drop(),
				CreateDatabase(),
				CreateTable(),
				Alter(),
				Insert(),
				DelimiterStatement(),
				EmptyStatement(),
				Spaces());
	}

	public Rule Newline() {
		return Sequence(Optional(Ch('\r')), Ch('\n'));
	}

	public Rule MultiLineCommentInitiator() {
		return Sequence(
				Ch('/'),
				Optional(Spaces()),
				Ch('*'));
	}

	public Rule MultiLineCommentTerminator() {
		return Sequence(
				Ch('*'),
				Optional(Spaces()),
				Ch('/'));
	}

	public Rule Comment() {
		return FirstOf(
				Sequence(
						FirstOf(Ch('#'), String("--")),
						ZeroOrMore(TestNot(Newline()), BaseParser.ANY),
						Newline()
				),
				Sequence(
						MultiLineCommentInitiator(),
						ZeroOrMore(TestNot(MultiLineCommentTerminator()), BaseParser.ANY),
						MultiLineCommentTerminator()
				));
	}

	public Rule Use() {
		return Sequence(
				IgnoreCase("use"),
				Spaces(),
				ZeroOrMore(TestNot(Delimiter()), BaseParser.ANY),
				EOL());
	}

	public Rule Set() {
		return Sequence(
				IgnoreCase("set"),
				Spaces(),
				ZeroOrMore(TestNot(Delimiter()), BaseParser.ANY),
				EOL());
	}

	public Rule Drop() {
		return Sequence(
				IgnoreCase("drop"),
				Spaces(),
				IgnoreCase("table"),
				ZeroOrMore(TestNot(Delimiter()), BaseParser.ANY),
				EOL());
	}

	public Rule Insert() {
		return Sequence(
				IgnoreCase("insert"),
				ZeroOrMore(TestNot(Delimiter()), BaseParser.ANY),
				EOL());
	}

	public Rule DelimiterStatement() {
		return Sequence(IgnoreCase("delimiter"), ZeroOrMore(TestNot(Delimiter()), BaseParser.ANY));
	}

	public Rule EmptyStatement() {
		return Delimiter();
	}

	public Rule DatabaseInstruction() {
		return FirstOf(IgnoreCase("database"), IgnoreCase("schema"));
	}

	public Rule CreateDatabaseInitiator() {
		return Sequence(
				IgnoreCase("create"),
				Spaces(),
				DatabaseInstruction());
	}

	public Rule CreateDatabase() {
		return Sequence(
				CreateDatabaseInitiator(),
				ZeroOrMore(TestNot(Delimiter()), BaseParser.ANY),
				EOL());
	}

	public Rule Something() {
		return Sequence(
				Spaces(),
				ZeroOrMore(NoneOf(" \t")),
				Spaces());
	}

	public Rule Alter() {
		return Sequence(
				IgnoreCase("alter"),
				Spaces(),
				IgnoreCase("table"),
				Something(),
				CommaSeparated(AlterSpecification()),
				EOL());
	}

	public Rule AlterSpecification() {
		return Sequence(
				IgnoreCase("add"),
				Spaces(),
				ForeignKeyDefinition());
	}

	public Rule ForeignKeyDefinition() {
		return Sequence(
				ForeignKeyDefinitionInitiator(),
				Spaces(),
				ParensFieldList(),
				Spaces(),
				ReferenceDefinition());
	}

	public Rule ForeignKeyDefinitionInitiator() {
		return FirstOf(
				Sequence(IgnoreCase("constraint"), Spaces(), IgnoreCase("foreign"), Spaces(),
						IgnoreCase("key"), Something()),
				Sequence(IgnoreCase("constraing"), Something(), IgnoreCase("foreign"), Spaces(),
						IgnoreCase("key")),
				Sequence(IgnoreCase("foreign"), Spaces(), IgnoreCase("key"), Optional(Something())));
	}

	public Rule ParensFieldList() {
		return Parenthetical(CommaSeparated(OneOrMore(NoneOf(")"))));
	}

	public Rule ReferenceDefinition() {
		return Sequence(
				IgnoreCase("references"),
				ZeroOrMore(TestNot(Delimiter()), BaseParser.ANY),
				Optional(ParensFieldList()));
	}

	public Rule CreateTable() {
		return Sequence(
				CreateTableInitiator(),
				Spaces(),
				TableName(),
				Optional(Spaces()),
				TableComponents(),
				Optional(Sequence(
						Optional(Spaces()),
						CreateTableOptions(),
						Optional(Spaces()))
				),
				EOL()

		);
	}

	public Rule CreateTableInitiator() {
		return Sequence(
				IgnoreCase("create"),
				Spaces(),
				Optional(Sequence(IgnoreCase("temporary"), Spaces())),
				IgnoreCase("table"),
				Optional(Sequence(Spaces(), IgnoreCase("if not exists"))));
	}

	public Rule TableName() {
		return FirstOf(QuotedIdentifier(), Identifier());
	}

	public Rule TableComponents() {
		return Parenthetical(CommaSeparated(CreateDefinition()));
	}

	public Rule CreateTableOptions() {
		return Sequence(
				FirstOf(CommentTableOption(), CharsetTableOption(), OtherTableOption()),
				ZeroOrMore(Sequence(
						Spaces(),
						FirstOf(CommentTableOption(), CharsetTableOption(), OtherTableOption()))
				));
	}

	public Rule CommentTableOption() {
		return Sequence(
				IgnoreCase("comment"),
				Optional(Spaces()),
				Ch('='),
				Optional(Spaces()),
				SingleQuoted());
	}

	public Rule CharsetTableOption() {
		return Sequence(
				Optional(IgnoreCase("default"), OneOrMore(Spaces())),
				FirstOf(IgnoreCase("charset"), IgnoreCase("character set")),
				Optional(Spaces()),
				Ch('='),
				Optional(Spaces()),
				Identifier());
	}

	public Rule OtherTableOption() {
		return Sequence(
				Identifier(),
				Optional(Spaces()),
				Ch('='),
				Optional(Spaces()),
				FirstOf(SingleQuoted(), DoubleQuoted(), Identifier()));
	}

	public Rule Parenthetical(Rule injection) {
		return Sequence(
				Ch('('),
				Optional(Spaces()),
				injection,
				Optional(Spaces()),
				Ch(')'));
	}

	public Rule CommaSeparated(Rule injection) {
		return Sequence(
				injection,
				ZeroOrMore(Sequence(
						Ch(','),
						Optional(Spaces()),
						injection
				)));
	}

	public Rule CreateDefinition() {
		return FirstOf(Index(), Field(), Comment());
	}

	public Rule Field() {
		return Sequence(
				ZeroOrMore(Comment()),
				FieldName(),
				Spaces(),
				FieldType(),
				Optional(Sequence(Spaces(), FieldQualifiers())),
				Optional(Sequence(Spaces(), FieldComment())),
				Optional(Sequence(Spaces(), ReferenceDefinition())),
				Optional(Sequence(Spaces(), OnUpdate())),
				Optional(Comment()));
	}

	public Rule FieldName() {
		return FirstOf(QuotedIdentifier(), Identifier());
	}

	public Rule FieldType() {
		return Sequence(
				FieldTypeName(),
				Optional(Sequence(
						Optional(Spaces()),
						Parenthetical(CommaSeparated(FieldValue()))
				)),
				ZeroOrMore(Sequence(Spaces(), TypeQualifier())));
	}

	public Rule FieldComment() {
		return Sequence(IgnoreCase("comment"), Spaces(), SingleQuoted());
	}

	// TODO
	public Rule OnUpdate() {
		return String("!!!TODO!!!");
	}

	public Rule TypeQualifier() {
		return FirstOf(IgnoreCase("binary"), IgnoreCase("unsigned"), IgnoreCase("zerofill"));
	}

	public Rule FieldValue() {
		return Value();
	}

	public Rule FieldTypeName() {
		return Identifier();
	}

	public Rule FieldQualifiers() {
		return Sequence(
				FieldQualifier(),
				ZeroOrMore(Spaces(), FieldQualifier()));
	}

	public Rule FieldQualifier() {
		return FirstOf(
				NotNullQualifier(),
				NullQualifier(),
				PrimaryKeyQualifier(),
				AutoIncrementQualifier(),
				CharacterSetQualifier(),
				CollateQualifier(),
				UniqueKeyQualifier(),
				DefaultQualifier());
	}

	public Rule NotNullQualifier() {
		return Sequence(IgnoreCase("not"), Spaces(), IgnoreCase("null"));
	}

	public Rule NullQualifier() {
		return IgnoreCase("null");
	}

	public Rule PrimaryKeyQualifier() {
		return FirstOf(
				IndexInstruction(),
				Sequence(IgnoreCase("primary"), Spaces(), IgnoreCase("key")));
	}

	public Rule AutoIncrementQualifier() {
		return IgnoreCase("auto_increment");
	}

	public Rule CharacterSetQualifier() {
		return Sequence(
				IgnoreCase("character"),
				Spaces(),
				IgnoreCase("set"),
				Spaces(),
				Identifier());
	}

	public Rule CollateQualifier() {
		return Sequence(IgnoreCase("collate"), Spaces(), Identifier());
	}

	public Rule UniqueKeyQualifier() {
		return Sequence(IgnoreCase("unique"), Spaces(), IndexInstruction());
	}

	public Rule DefaultQualifier() {
		return Sequence(IgnoreCase("default"), Spaces(), DefaultValue());
	}

	public Rule DefaultValue() {
		return FirstOf(CurrentTimestamp(), Text(), Bit(), UnclassifiedDefaultValue());
	}

	public Rule Text() {
		return FirstOf(
				Sequence(
						Ch('\''),
						ZeroOrMore(FirstOf(
								Sequence(Ch('\\'), BaseParser.ANY),
								String("\"\""),
								NoneOf("\\\"")
						)),
						Ch('\'')
				),
				Sequence(
						Ch('"'),
						ZeroOrMore(FirstOf(
								Sequence(Ch('\\'), BaseParser.ANY),
								String("\"\""),
								NoneOf("\\\"")
						)),
						Ch('"')
				));
	}

	public Rule Bit() {
		return Sequence(
				Ch('b'), FirstOf(
						Sequence(Ch('\''), OneOrMore(AnyOf("01")), Ch('\'')),
						Sequence(Ch('"'), OneOrMore(AnyOf("01")), Ch('"'))
				));
	}

	public Rule UnclassifiedDefaultValue() {
		return OneOrMore(FirstOf(
				CharRange('a', 'z'),
				CharRange('A', 'Z'),
				CharRange('0', '9'),
				AnyOf(":.-")));
	}

	public Rule CurrentTimestamp() {
		return FirstOf(IgnoreCase("current_timestamp()"), IgnoreCase("now()"));
	}

	public Rule Index() {
		return FirstOf(PrimaryKey(), UniqueKey(), ForeignKey(), NormalIndex(), FulltextIndex(),
				SpatialIndex());
	}

	public Rule PrimaryKey() {
		return Sequence(
				IgnoreCase("primary"),
				Spaces(),
				IgnoreCase("key"),
				OptionalIndexType(),
				Spaces(),
				Parenthetical(CommaSeparated(ColumnNameWithOptionalValues())),
				OptionalIndexType());
	}

	public Rule OptionalIndexType() {
		return Optional(Sequence(
				Spaces(),
				IndexType()));
	}

	// TODO
	public Rule UniqueKey() {
		return String("!!!TODO!!!");
	}

	// TODO
	public Rule ForeignKey() {
		return String("!!!TODO!!!");
	}

	public Rule IndexInstruction() {
		return FirstOf(IgnoreCase("key"), IgnoreCase("index"));
	}

	public Rule NormalIndex() {
		return Sequence(
				IndexInstruction(),
				Spaces(),
				IndexName(),
				OptionalUsingIndexType(),
				Spaces(),
				Parenthetical(CommaSeparated(ColumnNameWithOptionalValues())),
				OptionalUsingIndexType());
	}

	public Rule IndexName() {
		return Identifier();
	}

	public Rule OptionalUsingIndexType() {
		return Optional(Sequence(
				Spaces(),
				UsingIndexType()));
	}

	public Rule UsingIndexType() {
		return Sequence(
				IgnoreCase("using"),
				Spaces(),
				IndexType());
	}

	public Rule IndexType() {
		return FirstOf(IgnoreCase("btree"), IgnoreCase("hash"), IgnoreCase("rtree"));
	}

	public Rule ColumnNameWithOptionalValues() {
		return Sequence(
				ColumnName(),
				Optional(Sequence(
						Optional(Spaces()),
						Parenthetical(CommaSeparated(Value()))
				)));
	}

	public Rule FulltextIndex() {
		return Sequence(
				IgnoreCase("fulltext"),
				Spaces(),
				Optional(Sequence(IndexInstruction(), Spaces())),
				Optional(Sequence(IndexName(), Spaces())),
				Parenthetical(CommaSeparated(ColumnNameWithOptionalValues())));
	}

	public Rule SpatialIndex() {
		return Sequence(
				IgnoreCase("spatial"),
				Spaces(),
				Optional(IndexInstruction(), Spaces()),
				Optional(IndexName(), Spaces()),
				Parenthetical(CommaSeparated(ColumnNameWithOptionalValues())));
	}

	public Rule ColumnName() {
		return QuotedIdentifier();
	}

	public Rule Value() {
		return FirstOf(FloatNumber(), QuotedIdentifier(), String("NULL"));
	}

	public Rule FloatNumber() {
		return Sequence(BaseNumber(), Optional(ExponentNumber()));
	}

	public Rule Sign() {
		return AnyOf("-+");
	}

	public Rule UnsignedInteger() {
		return OneOrMore(CharRange('0', '9'));
	}

	public Rule BaseNumber() {
		return Sequence(Optional(Sign()), Optional(Ch('.')), UnsignedInteger());
	}

	public Rule ExponentNumber() {
		return Sequence(
				AnyOf("eE"),
				UnsignedInteger());
	}

	public Rule Identifier() {
		return OneOrMore(NoneOf(" \t"));
	}

	public Rule QuotedIdentifier() {
		return FirstOf(SingleQuoted(), DoubleQuoted(), BackQuoted());
	}

	public Rule SingleQuoted() {
		return Sequence(Ch('\''), OneOrMore(NoneOf("'")), Ch('\''));
	}

	public Rule DoubleQuoted() {
		return Sequence(Ch('"'), OneOrMore(NoneOf("\"")), Ch('"'));
	}

	public Rule BackQuoted() {
		return Sequence(Ch('`'), OneOrMore(NoneOf("`")), Ch('`'));
	}

	public Rule Spaces() {
		return OneOrMore(AnyOf(" \t"));
	}

	public Rule Delimiter() {
		return Ch(';');
	}

	public Rule EOL() {
		return Sequence(Delimiter(), Optional(Spaces()));
	}
}
