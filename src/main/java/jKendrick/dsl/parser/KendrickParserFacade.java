package jKendrick.dsl.parser;

import jKendrick.dsl.ast.Ast;
import jKendrick.dsl.grammar.KendrickBaseVisitor;
import jKendrick.dsl.grammar.KendrickLexer;
import jKendrick.dsl.grammar.KendrickParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry point of the Kendrick DSL front end: text in, {@link Ast.Script} out.
 *
 * Syntax errors are collected and reported together in a {@link KendrickSyntaxException};
 * nothing is checked beyond the grammar here.
 */
public final class KendrickParserFacade {

    private KendrickParserFacade() {}

    public static Ast.Script parseFile(Path path) throws IOException {
        return parse(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), path.toString());
    }

    public static Ast.Script parse(String source) {
        return parse(source, "<string>");
    }

    public static Ast.Script parse(String source, String sourceName) {
        CharStream input = CharStreams.fromString(source, sourceName);
        KendrickLexer lexer = new KendrickLexer(input);
        ErrorCollector errors = new ErrorCollector(sourceName);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errors);
        KendrickParser parser = new KendrickParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errors);
        KendrickParser.ScriptContext tree = parser.script();
        errors.throwIfAny();
        return new AstBuilder().visitScript(tree);
    }

    // ------------------------------------------------------------------ errors

    /** All syntax errors of one parse, with line and column. */
    public static final class KendrickSyntaxException extends RuntimeException {
        public final List<String> errors;
        KendrickSyntaxException(List<String> errors) {
            super(String.join("\n", errors));
            this.errors = errors;
        }
    }

    private static final class ErrorCollector extends BaseErrorListener {
        private final String sourceName;
        private final List<String> messages = new ArrayList<>();
        ErrorCollector(String sourceName) { this.sourceName = sourceName; }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                int charPositionInLine, String msg, RecognitionException e) {
            messages.add(sourceName + ":" + line + ":" + (charPositionInLine + 1) + ": " + msg);
        }

        void throwIfAny() {
            if (!messages.isEmpty()) throw new KendrickSyntaxException(messages);
        }
    }

    // ------------------------------------------------------------------ parse tree -> AST

    private static final class AstBuilder extends KendrickBaseVisitor<Object> {
        /** Records where a node starts in the script (ANTLR columns are 0-based). */
        @SuppressWarnings("unchecked")
        private static <N extends Ast.Node> N at(N node, org.antlr.v4.runtime.ParserRuleContext ctx) {
            return (N) node.at(ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine() + 1);
        }


        @Override
        public Ast.Script visitScript(KendrickParser.ScriptContext ctx) {
            List<Ast.Entity> entities = new ArrayList<>();
            for (KendrickParser.EntityContext e : ctx.entity()) entities.add((Ast.Entity) visit(e));
            return new Ast.Script(entities);
        }

        @Override
        public Object visitEntity(KendrickParser.EntityContext ctx) {
            return visit(ctx.getChild(0));
        }

        // ---- KModel

        @Override
        public Ast.KModel visitKmodel(KendrickParser.KmodelContext ctx) {
            Ast.Attribute attribute = null;
            List<String> parameters = new ArrayList<>();
            Map<String, Ast.Expr> formulas = new LinkedHashMap<>();
            List<Ast.Equation> equations = new ArrayList<>();
            List<Ast.Transition> transitions = new ArrayList<>();
            double[][] matrix = null;
            Ast.Topology topology = null;
            Ast.Expr transitionRate = null;
            String entry = null;
            boolean inherited = false;

            for (KendrickParser.ModelClauseContext c : ctx.modelClause()) {
                if (c instanceof KendrickParser.AttributeClauseContext) {
                    KendrickParser.AttributeClauseContext a = (KendrickParser.AttributeClauseContext) c;
                    attribute = new Ast.Attribute(a.attr.getText(), domainValues(a.domain()));
                } else if (c instanceof KendrickParser.ParametersClauseContext) {
                    for (TerminalNode id : ((KendrickParser.ParametersClauseContext) c).ID()) parameters.add(id.getText());
                } else if (c instanceof KendrickParser.EquationsClauseContext) {
                    for (KendrickParser.EquationContext eq : ((KendrickParser.EquationsClauseContext) c).equation())
                        equations.add(at(new Ast.Equation(keywordName(eq.compartment.getText()), expr(eq.expr())), eq));
                } else if (c instanceof KendrickParser.TransitionsClauseContext) {
                    for (KendrickParser.TransitionContext t : ((KendrickParser.TransitionsClauseContext) c).transition())
                        transitions.add(at(new Ast.Transition(t.from.getText(), expr(t.rate), t.to.getText()), t));
                } else if (c instanceof KendrickParser.ConnectionMatrixClauseContext) {
                    List<KendrickParser.RowContext> rows = ((KendrickParser.ConnectionMatrixClauseContext) c).row();
                    matrix = new double[rows.size()][];
                    for (int i = 0; i < rows.size(); i++) {
                        List<TerminalNode> nums = rows.get(i).NUMBER();
                        matrix[i] = new double[nums.size()];
                        for (int j = 0; j < nums.size(); j++) matrix[i][j] = Double.parseDouble(nums.get(j).getText());
                    }
                } else if (c instanceof KendrickParser.TopologyClauseContext) {
                    KendrickParser.TopologyClauseContext t = (KendrickParser.TopologyClauseContext) c;
                    topology = at(new Ast.Topology(t.kind.getText(), t.direction == null ? null : t.direction.getText()), t);
                } else if (c instanceof KendrickParser.EntryClauseContext) {
                    entry = ((KendrickParser.EntryClauseContext) c).entryValue.getText();
                } else if (c instanceof KendrickParser.InheritedClauseContext) {
                    inherited = true;
                } else if (c instanceof KendrickParser.TransitionRateClauseContext) {
                    transitionRate = expr(((KendrickParser.TransitionRateClauseContext) c).expr());
                } else if (c instanceof KendrickParser.ParameterFormulaClauseContext) {
                    KendrickParser.ParameterFormulaClauseContext p = (KendrickParser.ParameterFormulaClauseContext) c;
                    formulas.put(keywordName(p.KEYWORD().getText()), expr(p.expr()));
                }
            }
            return at(new Ast.KModel(ctx.name.getText(), attribute, parameters, formulas, equations, transitions, matrix, topology, transitionRate, entry, inherited), ctx);
        }

        private static List<String> domainValues(KendrickParser.DomainContext d) {
            List<String> values = new ArrayList<>();
            if (d instanceof KendrickParser.EnumeratedDomainContext) {
                for (KendrickParser.ValueContext v : ((KendrickParser.EnumeratedDomainContext) d).value()) values.add(v.getText());
            } else {
                KendrickParser.RangeDomainContext r = (KendrickParser.RangeDomainContext) d;
                int lo = Integer.parseInt(r.lo.getText()), hi = Integer.parseInt(r.hi.getText());
                for (int i = lo; i <= hi; i++) values.add(Integer.toString(i));
            }
            return values;
        }

        // ---- Composition

        @Override
        public Ast.Composition visitComposition(KendrickParser.CompositionContext ctx) {
            List<String> models = new ArrayList<>();
            List<Map<String, String>> renamings = new ArrayList<>();
            for (KendrickParser.ModelRefContext m : ctx.modelRef()) {
                models.add(string(m.STRING()));
                Map<String, String> renaming = new LinkedHashMap<>();
                for (KendrickParser.RenamePairContext p : m.renamePair()) {
                    String from = p.from.getText(), to = p.to.getText();
                    if (renaming.put(from, to) != null)
                        throw new KendrickSyntaxException(List.of("line " + p.getStart().getLine() + ": Composition "
                                + ctx.name.getText() + ": '" + from + "' is renamed twice"));
                }
                renamings.add(renaming);
            }
            return at(new Ast.Composition(ctx.name.getText(), models, renamings), ctx);
        }

        // ---- Scenario

        @Override
        public Ast.Scenario visitScenario(KendrickParser.ScenarioContext ctx) {
            List<Ast.Binding> bindings = new ArrayList<>();
            List<Ast.Assignment> assignments = new ArrayList<>();
            for (KendrickParser.AssignmentContext a : ctx.assignment()) {
                if (a instanceof KendrickParser.VarAssignmentContext) {
                    for (KendrickParser.BindingContext b : ((KendrickParser.VarAssignmentContext) a).binding())
                        bindings.add(new Ast.Binding(b.var.getText(), b.attr.getText()));
                } else if (a instanceof KendrickParser.ArrayAssignmentContext) {
                    KendrickParser.ArrayAssignmentContext v = (KendrickParser.ArrayAssignmentContext) a;
                    assignments.add(at(new Ast.ValueAssignment(target(v.KEYWORD()), numberArray(v.numberArray())), v));
                } else if (a instanceof KendrickParser.ScalarAssignmentContext) {
                    KendrickParser.ScalarAssignmentContext v = (KendrickParser.ScalarAssignmentContext) a;
                    assignments.add(at(new Ast.ValueAssignment(target(v.KEYWORD()),
                            new Ast.NumberArray(Double.parseDouble(v.NUMBER().getText()))), v));
                } else {
                    KendrickParser.FormulaAssignmentContext f = (KendrickParser.FormulaAssignmentContext) a;
                    assignments.add(at(new Ast.FormulaAssignment(target(f.KEYWORD()), expr(f.expr())), f));
                }
            }
            return at(new Ast.Scenario(ctx.name.getText(), unquote(ctx.target.getText()), bindings, assignments), ctx);
        }

        private static Ast.Indexed target(TerminalNode keyword) {
            return Ast.Indexed.parse(keywordName(keyword.getText()));
        }

        private static Ast.NumberArray numberArray(KendrickParser.NumberArrayContext ctx) {
            List<Ast.NumberArray> elements = new ArrayList<>();
            if (!ctx.NUMBER().isEmpty()) {
                for (TerminalNode n : ctx.NUMBER()) elements.add(new Ast.NumberArray(Double.parseDouble(n.getText())));
            } else {
                for (KendrickParser.NumberArrayContext sub : ctx.numberArray()) elements.add(numberArray(sub));
            }
            return new Ast.NumberArray(elements);
        }

        // ---- Simulation / Visualization

        @Override
        public Ast.Simulation visitSimulation(KendrickParser.SimulationContext ctx) {
            return at(new Ast.Simulation(ctx.name.getText(), keywordArgs(ctx.keywordArg())), ctx);
        }

        @Override
        public Ast.Visualization visitVisualization(KendrickParser.VisualizationContext ctx) {
            return at(new Ast.Visualization(ctx.name.getText(), keywordArgs(ctx.keywordArg())), ctx);
        }

        private static Map<String, Ast.Literal> keywordArgs(List<KendrickParser.KeywordArgContext> args) {
            Map<String, Ast.Literal> map = new LinkedHashMap<>();
            for (KendrickParser.KeywordArgContext a : args) map.put(keywordName(a.KEYWORD().getText()), literal(a.literal()));
            return map;
        }

        private static Ast.Literal literal(KendrickParser.LiteralContext ctx) {
            if (ctx.NUMBER() != null) return Ast.Literal.number(Double.parseDouble(ctx.NUMBER().getText()));
            if (ctx.STRING() != null) return Ast.Literal.string(string(ctx.STRING()));
            if (ctx.ID() != null) return Ast.Literal.identifier(ctx.ID().getText());
            List<Ast.Literal> elements = new ArrayList<>();
            for (KendrickParser.LiteralContext sub : ctx.literal()) elements.add(literal(sub));
            return Ast.Literal.array(elements);
        }

        // ---- expressions

        private static Ast.Expr expr(KendrickParser.ExprContext ctx) {
            if (ctx instanceof KendrickParser.NumberExprContext)
                return new Ast.Num(Double.parseDouble(ctx.getText()));
            if (ctx instanceof KendrickParser.NameExprContext || ctx instanceof KendrickParser.TimeExprContext)
                return new Ast.Name(Ast.Indexed.parse(ctx.getText())); // the time is the name t
            if (ctx instanceof KendrickParser.ParenExprContext)
                return expr(((KendrickParser.ParenExprContext) ctx).expr());
            if (ctx instanceof KendrickParser.NegExprContext)
                return new Ast.Neg(expr(((KendrickParser.NegExprContext) ctx).expr()));
            if (ctx instanceof KendrickParser.MulExprContext) {
                KendrickParser.MulExprContext m = (KendrickParser.MulExprContext) ctx;
                return new Ast.Binary(m.op.getText().charAt(0), expr(m.expr(0)), expr(m.expr(1)));
            }
            if (ctx instanceof KendrickParser.AddExprContext) {
                KendrickParser.AddExprContext a = (KendrickParser.AddExprContext) ctx;
                return new Ast.Binary(a.op.getText().charAt(0), expr(a.expr(0)), expr(a.expr(1)));
            }
            if (ctx instanceof KendrickParser.CallExprContext) {
                KendrickParser.CallExprContext c = (KendrickParser.CallExprContext) ctx;
                List<Ast.Expr> args = new ArrayList<>();
                for (KendrickParser.ExprContext e : c.expr()) args.add(expr(e));
                return new Ast.Call(c.func.getText(), args);
            }
            KendrickParser.SumExprContext s = (KendrickParser.SumExprContext) ctx;
            return new Ast.Sum(s.var.getText(), s.attr.getText(), expr(s.expr()));
        }

        // ---- lexical helpers

        /** {@code "lambda_pk:"} -> {@code "lambda_pk"}. */
        private static String keywordName(String keyword) {
            return keyword.substring(0, keyword.length() - 1);
        }

        /** {@code 'it''s'} -> {@code it's}. */
        private static String string(TerminalNode node) {
            return unquote(node.getText());
        }

        private static String unquote(String t) {
            return t.substring(1, t.length() - 1).replace("''", "'");
        }
    }
}
