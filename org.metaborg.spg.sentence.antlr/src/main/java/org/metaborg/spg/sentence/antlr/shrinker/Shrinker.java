package org.metaborg.spg.sentence.antlr.shrinker;

import org.metaborg.spg.sentence.antlr.generator.Generator;
import org.metaborg.spg.sentence.antlr.tree.Leaf;
import org.metaborg.spg.sentence.antlr.tree.Node;
import org.metaborg.spg.sentence.antlr.tree.Tree;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static org.metaborg.spg.sentence.antlr.functional.Utils.lift;

public class Shrinker {
    private final Random random;
    private final Generator generator;

    public Shrinker(Random random, Generator generator) {
        this.random = random;
        this.generator = generator;
    }

    public Stream<Tree> shrink(Tree tree) {
        List<Node> subtrees = subtrees(tree).collect(Collectors.toList());
        Collections.shuffle(subtrees, random);

        return subtrees.stream().flatMap(subTree ->
                shrink(tree, subTree)
        );
    }

    private Stream<Tree> shrink(Tree tree, Node subTree) {
        int size = size(subTree);
        Optional<Tree> replacementOpt = generator.forElement(subTree.getElementOpt(), size - 1);

        return lift(replacementOpt.map(replacement ->
                replace(tree, subTree, replacement)
        ));
    }

    private Tree replace(Tree tree, Node subTree, Tree replacement) {
        if (tree == subTree) {
            return replacement;
        }

        if (tree instanceof Node) {
            Node node = (Node) tree;

            Tree[] children = Arrays
                    .stream(tree.getChildren())
                    .map(child -> replace(child, subTree, replacement))
                    .toArray(Tree[]::new);

            return new Node(node.getElementOpt(), children);
        } else if (tree instanceof Leaf) {
            return tree;
        }

        throw new IllegalArgumentException("Unexpected tree (neither node nor leaf).");
    }

    private int size(Tree subTree) {
        return 1 + Arrays
                .stream(subTree.getChildren())
                .mapToInt(this::size)
                .sum();
    }

    public Stream<Node> subtrees(Tree tree) {
        if (tree instanceof Leaf) {
            return empty();
        }

        Stream<Node> children = Arrays
                .stream(tree.getChildren())
                .flatMap(this::subtrees);

        return concat(of((Node) tree), children);
    }
}
