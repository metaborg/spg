mkdir tmp
cd tmp

# metaborg-units
git clone git@github.com:spg-subjects/metaborg-units.git
(cd metaborg-units; git checkout 5e9574b)
(cd metaborg-units/org.metaborg.lang.appfunc; mvn clean package)
(cd metaborg-units/org.metaborg.lang.mixml; mvn clean package)
(cd metaborg-units/org.metaborg.lang.sml; mvn clean package)
(cd metaborg-units/org.metaborg.lang.stsrm; mvn clean package)
(cd metaborg-units/org.metaborg.lang.units; mvn clean package)

# metaborg-calc
git clone git@github.com:spg-subjects/metaborg-calc.git
(cd metaborg-calc/org.metaborg.lang.calc; git checkout 66df966; mvn clean package)

# metaborg-sl
git clone git@github.com:spg-subjects/metaborg-sl.git
(cd metaborg-sl/org.metaborg.lang.sl; git checkout 93e8e47; mvn clean package)

# metaborg-whilelang
git clone git@github.com:spg-subjects/metaborg-whilelang.git
(cd metaborg-whilelang/org.metaborg.lang.whilelang; git checkout 683e4d5; mvn clean package)

# metaborg-js
git clone git@github.com:spg-subjects/metaborg-js.git
(cd metaborg-js/spoofaxJS; git checkout fe1aabc; mvn clean package)

# metaborg-typescript
git clone git@github.com:spg-subjects/metaborg-typescript.git
(cd metaborg-typescript/typescript; git checkout 35d7bc7; mvn clean package)

# metaborg-stratego
git clone git@github.com:spg-subjects/stratego.git
(cd stratego/org.metaborg.meta.lang.stratego.typed; git checkout f900b6e; mvn clean package)

# sdf3-demo
git clone git@github.com:spg-subjects/sdf3-demo.git
(cd sdf3-demo/sdf3-demo; git checkout db4cb2e; mvn clean package)

# metaborg-tiger
git clone git@github.com:spg-subjects/metaborg-tiger.git
(cd metaborg-tiger/org.metaborg.lang.tiger/correct; git checkout 1743d5c; mvn clean package)

# metaborg-jasmin
git clone git@github.com:spg-subjects/metaborg-jasmin.git
(cd metaborg-jasmin/jasmin; git checkout 66411e1; mvn clean package)

# metaborg-pascal
git clone git@github.com:spg-subjects/metaborg-pascal.git
(cd metaborg-pascal/org.metaborg.lang.pascal; git checkout c54654a; mvn clean package)

# metaborg-llir
git clone git@github.com:spg-subjects/metaborg-llir.git
(cd metaborg-llir/metaborg-llir; git checkout 0b422ea; mvn clean package)

# metaborg-smalltalk
git clone git@github.com:spg-subjects/metaborg-smalltalk.git
(cd metaborg-smalltalk/Smalltalk; git checkout b216ca3; mvn clean package)

# metaborg-coq
git clone git@github.com:spg-subjects/metaborg-coq.git
(cd metaborg-coq/org.metaborg.lang.coq; git checkout e224d2c; mvn clean package)

# metaborg-grace
git clone git@github.com:spg-subjects/metaborg-grace.git
(cd metaborg-grace/grace; git checkout 211c98e; mvn clean package)

# java-front/java-1.7
git clone git@github.com:spg-subjects/java-front.git java-front-17
(cd java-front-17/lang.java; git checkout 71f436e; mvn clean package)

# java-front/java-1.8
git clone git@github.com:spg-subjects/java-front.git java-front-18
(cd java-front-18/lang.java; git checkout 35107e7; mvn clean package)

