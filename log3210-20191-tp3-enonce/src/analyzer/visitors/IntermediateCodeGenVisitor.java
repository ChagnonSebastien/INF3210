package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created: 19-02-15
 * Last Changed: 19-02-17
 * Author: Félix Brunet
 *
 * Description: Ce visiteur explore l'AST et génère un code intermédiaire.
 */

public class IntermediateCodeGenVisitor implements ParserVisitor {

    //le m_writer est un Output_Stream connecter au fichier "result". c'est donc ce qui permet de print dans les fichiers
    //le code généré.
    private final PrintWriter m_writer;

    public IntermediateCodeGenVisitor(PrintWriter writer) {
        m_writer = writer;
    }
    public HashMap<String, VarType> SymbolTable = new HashMap<>();

    private int id = 0;
    private int label = 0;
    /*
    génère une nouvelle variable temporaire qu'il est possible de print
    À noté qu'il serait possible de rentrer en conflit avec un nom de variable définit dans le programme.
    Par simplicité, dans ce tp, nous ne concidérerons pas cette possibilité, mais il faudrait un générateur de nom de
    variable beaucoup plus robuste dans un vrai compilateur.
     */
    private String genId() {
        return "_t" + id++;
    }

    //génère un nouveau Label qu'il est possible de print.
    private String genLabel() {
        return "_L" + label++;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data)  {
        for (int i = 0; i < node.jjtGetNumChildren() - 1; i++)
            node.jjtGetChild(i).jjtAccept(this, data);

        String SNext = genLabel();

        // Initialisation
        Map<String, String> SChildMap = new HashMap<>();
        SChildMap.put("SNext", SNext);

        // Execution
        node.jjtGetChild(node.jjtGetNumChildren() - 1).jjtAccept(this, SChildMap);
        m_writer.println(SNext);
        return null;
    }

    /*
    Code fournis pour remplir la table de symbole.
    Les déclarations ne sont plus utile dans le code à trois adresse.
    elle ne sont donc pas concervé.
     */
    @Override
    public Object visit(ASTDeclaration node, Object data) {
        ASTIdentifier id = (ASTIdentifier) node.jjtGetChild(0);
        VarType t;
        if(node.getValue().equals("bool")) {
            t = VarType.Bool;
        } else {
            t = VarType.Number;
        }
        SymbolTable.put(id.getValue(), t);
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        Map<String, String> parentMap = (HashMap<String, String>) data;

        for(int i = 0; i < node.jjtGetNumChildren() - 1; i++){
            String SiNext = genLabel();

            // Initialisation
            Map<String, String> SiChildMap = new HashMap<>();
            SiChildMap.put("SNext", SiNext);

            // Execution
            node.jjtGetChild(i).jjtAccept(this, SiChildMap);
            m_writer.println(SiNext);
        }

        // Initialisation
        Map<String, String> SnChildMap = new HashMap<>();
        SnChildMap.put("SNext", parentMap.get("SNext"));

        // Execution
        if(node.jjtGetNumChildren() > 0)
            node.jjtGetChild(node.jjtGetNumChildren() - 1).jjtAccept(this, SnChildMap);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    /*
    le If Stmt doit vérifier s'il à trois enfants pour savoir s'il s'agit d'un "if-then" ou d'un "if-then-else".
     */
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        Map<String, String> parentMap = (HashMap<String, String>) data;

        if(node.jjtGetNumChildren() == 2){
            String BTrue = genLabel();

            // Initialisation
            Map<String, String> BChildMap = new HashMap<>();
            BChildMap.put("BTrue", BTrue);
            BChildMap.put("BFalse", parentMap.get("SNext"));

            Map<String, String> SChildMap = new HashMap<>();
            SChildMap.put("SNext", parentMap.get("SNext"));

            // Ecriture
            node.jjtGetChild(0).jjtAccept(this, BChildMap);
            m_writer.println(BTrue);
            node.jjtGetChild(1).jjtAccept(this, SChildMap);
        }
        else {
            String BTrue = genLabel();
            String BFalse = genLabel();

            // Initialisation
            Map<String, String> BChildMap = new HashMap<>();
            BChildMap.put("BTrue", BTrue);
            BChildMap.put("BFalse", BFalse);

            Map<String, String> S1ChildMap = new HashMap<>();
            S1ChildMap.put("SNext", parentMap.get("SNext"));

            Map<String, String> S2ChildMap = new HashMap<>();
            S2ChildMap.put("SNext", parentMap.get("SNext"));

            // Ecriture
            node.jjtGetChild(0).jjtAccept(this, BChildMap);
            m_writer.println(BTrue);
            node.jjtGetChild(1).jjtAccept(this, S1ChildMap);
            m_writer.println("goto " + parentMap.get("SNext"));
            m_writer.println(BFalse);
            node.jjtGetChild(2).jjtAccept(this, S2ChildMap);
        }
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        Map<String, String> parentMap = (HashMap<String, String>) data;

        String Begin = genLabel();
        String BTrue = genLabel();

        // Initialisation
        Map<String, String> BChildMap = new HashMap<>();
        BChildMap.put("BTrue", BTrue);
        BChildMap.put("BFalse", parentMap.get("SNext"));

        Map<String, String> SChildMap = new HashMap<>();
        SChildMap.put("SNext", "SNext");

        // Ecriture
        m_writer.println(Begin);
        node.jjtGetChild(0).jjtAccept(this, BChildMap);
        m_writer.println(BTrue);
        node.jjtGetChild(1).jjtAccept(this, SChildMap);
        m_writer.println("goto " + Begin);
        return null;
    }

    /*
     *  la difficulté est d'implémenter le "short-circuit" des opérations logiques combinez à l'enregistrement des
     *  valeurs booléennes dans des variables.
     *
     *  par exemple,
     *  a = b || c && !d
     *  deviens
     *  if(b)
     *      t1 = 1
     *  else if(c)
     *      if(d)
     *         t1 = 1
     *      else
     *         t1 = 0
     *  else
     *      t1 = 0
     *  a = t1
     *
     *  qui est équivalent à :
     *
     *  if b goto LTrue
     *  ifFalse c goto LFalse
     *  ifFalse d goto LTrue
     *  goto LFalse
     *  //Assign
     *  LTrue
     *  a = 1
     *  goto LEnd
     *  LFalse
     *  a = 0
     *  LEnd
     *  //End Assign
     *
     *  mais
     *
     *  a = 1 * 2 + 3
     *
     *  deviens
     *
     *  //expr
     *  t1 = 1 * 2
     *  t2 = t1 + 3
     *  //expr
     *  a = t2
     *
     *  et
     *
     *  if(b || c && !d)
     *
     *  deviens
     *
     *  //expr
     *  if b goto LTrue
     *  ifFalse c goto LFalse
     *  ifFalse d goto LTrue
     *  goto LFalse
     *  //expr
     *  //if
     *  LTrue
     *  codeS1
     *  goto lEnd
     *  LFalse
     *  codeS2
     *  LEnd
     *  //end if
     *
     *
     *  Il faut donc dès le départ vérifier dans la table de symbole le type de la variable à gauche, et générer du
     *  code différent selon ce type.
     *
     *  Pour avoir l'id de la variable de gauche de l'assignation, il peut être plus simple d'aller chercher la valeur
     *  du premier enfant sans l'accepter.
     *  De la sorte, on accept un noeud "Identifier" seulement lorsqu'on l'utilise comme référence (à droite d'une assignation)
     *  Cela simplifie le code de part et d'autre.
     *
     *
     *  Aussi, il peut être pertinent d'extraire le code de l'assignation dans une fonction privé, parce que ce code
     *  sera utile pour les noeuds de comparaison (plus d'explication au commentaire du noeud en question.)
     *  la signature de la fonction que j'ai utilisé pour se faire est :
     *  private String generateAssignCode(Node node, String tId);
     *  ou "node" est le noeud de l'expression représentant la valeur, et tId est le nom de la variable ou assigné
     *  la valeur.
     *
     *  Il est normal (et probablement inévitable concidérant la structure de l'arbre)
     *  de générer inutilement des labels (ou des variables temporaire) qui ne sont pas utilisé ni imprimé dans le code résultant.
     */
    @Override
    public Object visit(ASTAssignStmt node, Object data) {

        String id = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        VarType type = SymbolTable.get(id);
        if(type == VarType.Number){
            Map<String, String> EchildMap = (HashMap<String, String>) node.jjtGetChild(1).jjtAccept(this, data);
            m_writer.println(id + " = " + EchildMap.get("EAddr"));
        }
        else {
            Map<String, String> BchildMap =  new HashMap<>();
            String BTrue = genLabel();
            String BFalse = genLabel();
            String SNext = genLabel();

            BchildMap.put("BTrue", BTrue);
            BchildMap.put("BFalse", BFalse);

            node.jjtGetChild(1).jjtAccept(this, BchildMap);
            m_writer.println(BTrue);
            m_writer.println(id + " = 1");
            m_writer.println("goto " + SNext);

            m_writer.println(BFalse);
            m_writer.println(id + " = 0");
            m_writer.println(SNext);
        }

        return null;
    }


    //Il n'y a probablement rien à faire ici
    @Override
    public Object visit(ASTExpr node, Object data){
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    //Expression arithmétique
    /*
    Les expressions arithmétique add et mult fonctionne exactement de la même manière. c'est pourquoi
    il est plus simple de remplir cette fonction une fois pour avoir le résultat pour les deux noeuds.

    On peut bouclé sur "ops" ou sur node.jjtGetNumChildren(),
    la taille de ops sera toujours 1 de moins que la taille de jjtGetNumChildren
     */
    public String exprCodeGen(SimpleNode node, Object data, Vector<String> ops) {

        Map<String, String> EchildMap = (HashMap<String, String>) node.jjtGetChild(0).jjtAccept(this, data);
        String lastResult = EchildMap.get("EAddr");

        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            EchildMap = (HashMap<String, String>) node.jjtGetChild(i).jjtAccept(this, data);
            String EAddr = genId();
            m_writer.println(EAddr + " = " + lastResult + " " + ops.get(i - 1) + " " + EchildMap.get("EAddr"));
            lastResult = EAddr;
        }

        return lastResult;
    }

    @Override
    public Object visit(ASTAddExpr node, Object data) {
        if(node.jjtGetNumChildren() == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        }

        Map<String, String> returnMap = new HashMap<>();
        returnMap.put("EAddr", exprCodeGen(node, data, node.getOps()));
        return returnMap;
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        if(node.jjtGetNumChildren() == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        Map<String, String> returnMap = new HashMap<>();
        returnMap.put("EAddr", exprCodeGen(node, data, node.getOps()));
        return returnMap;
    }

    //UnaExpr est presque pareil au deux précédente. la plus grosse différence est qu'il ne va pas
    //chercher un deuxième noeud enfant pour avoir une valeur puisqu'il s'agit d'une opération unaire.
    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        String EAddr = genId();
        Map<String, String> EchildMap = (HashMap<String, String>) node.jjtGetChild(0).jjtAccept(this, data);
        if(EchildMap == null){
            return null;
        }
        if(node.getOps().size() % 2 == 1) {
            m_writer.println(EAddr + " = -" + EchildMap.get("EAddr"));
            Map<String, String> returnMap = new HashMap<>();
            returnMap.put("EAddr", EAddr);
            return returnMap;
        } else {
            return EchildMap;
        }
    }

    //expression logique

    /*

    Rappel, dans le langague, le OU et le ET on la même priorité, et sont associatif à droite par défaut.
    ainsi :
    "a = a || a2 || b && c || d" est interprété comme "a = a || a2 || (b && (c || d))"

    Cette fonction est parmis les plus complexes, en particulier si on prend en compte la technique du "fall".
     */
    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        Map<String, String> parentMap = (HashMap<String, String>) data;

        for(int i = 0; i < node.getOps().size(); i++){
            // Initialisation
            Map<String, String> BChildMap = new HashMap<>();

            String BTemp = genLabel();
            if(node.getOps().get(i).equals("&&")){
                BChildMap.put("BFalse", parentMap.get("BFalse"));
                BChildMap.put("BTrue", BTemp);
            }
            else {
                BChildMap.put("BTrue", parentMap.get("BTrue"));
                BChildMap.put("BFalse", BTemp);
            }

            // Ecriture
            node.jjtGetChild(i).jjtAccept(this, BChildMap);
            m_writer.println(BTemp);
        }
        Map<String, String> BChildMap = new HashMap<>();
        BChildMap.put("BFalse", parentMap.get("BFalse"));
        BChildMap.put("BTrue", parentMap.get("BTrue"));
        return node.jjtGetChild(node.jjtGetNumChildren() - 1).jjtAccept(this, BChildMap);
    }


    //cette fonction privé est utile parce que le code pour généré le goto pour les opérateurs de comparaison est le même
    //que celui pour le référencement de variable booléenne.
    //le code est très simple avant l'optimisation, mais deviens un peu plus long avec l'optimisation.
    private void genCodeRelTestJump(String labelTrue, String labelfalse, String test) {
        //version sans optimisation.
        m_writer.println("if " + test + " goto " + labelTrue);
        m_writer.println("goto " + labelfalse);
    }


    //une partie de la fonction à été faite pour donner des pistes, mais comme tous le reste du fichier, tous est libre
    //à modification.
    /*
    À ajouté : la comparaison est plus complexe quand il s'agit d'une comparaison de booléen.
    Le truc est de :
    1. vérifier qu'il s'agit d'une comparaison de nombre ou de booléen.
        On peut Ce simplifier la vie et le déterminer simplement en regardant si les enfants retourne une valeur ou non, à condition
        de s'être assurer que les valeurs booléennes retourne toujours null.
     2. s'il s'agit d'une comparaison de nombre, on peut faire le code simple par "genCodeRelTestJump(B, test)"
     3. s'il s'agit d'une comparaison de booléen, il faut enregistrer la valeur gauche et droite de la comparaison dans une variable temporaire,
        en utilisant le même code que pour l'assignation, deux fois. (mettre ce code dans une fonction deviens alors pratique)
        avant de faire la comparaison "genCodeRelTestJump(B, test)" avec les deux variables temporaire.

        notez que cette méthodes peut sembler peu efficace pour certain cas, mais qu'avec des passes d'optimisations subséquente, (que l'on
        ne fera pas dans le cadre du TP), on pourrait s'assurer que le code produit est aussi efficace qu'il peut l'être.
     */
    @Override
    public Object visit(ASTCompExpr node, Object data) {
        if(node.jjtGetNumChildren() == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        }

        Map<String, String> parentMap = (HashMap<String, String>) data;

        String id1 = genId();
        Map<String, String> B1childMap =  new HashMap<>();
        String B1True = genLabel();
        String B1False = genLabel();
        String S1Next = genLabel();

        B1childMap.put("BTrue", B1True);
        B1childMap.put("BFalse", B1False);

        Map<String, String> EchildMap1 = (HashMap<String, String>) node.jjtGetChild(0).jjtAccept(this, B1childMap);

        if(EchildMap1 != null){
            Map<String, String> EchildMap2 = (HashMap<String, String>) node.jjtGetChild(1).jjtAccept(this, data);
            genCodeRelTestJump(parentMap.get("BTrue"), parentMap.get("BFalse"), EchildMap1.get("EAddr") + " " + node.getValue() + " " + EchildMap2.get("EAddr"));
        }
        else {
            m_writer.println(B1True);
            m_writer.println(id1 + " = 1");
            m_writer.println("goto " + S1Next);

            m_writer.println(B1False);
            m_writer.println(id1 + " = 0");
            m_writer.println(S1Next);

            String id2 = genId();
            Map<String, String> B2childMap =  new HashMap<>();
            String B2True = genLabel();
            String B2False = genLabel();
            String S2Next = genLabel();

            B2childMap.put("BTrue", B2True);
            B2childMap.put("BFalse", B2False);

            node.jjtGetChild(1).jjtAccept(this, B2childMap);
            m_writer.println(B2True);
            m_writer.println(id2 + " = 1");
            m_writer.println("goto " + S2Next);

            m_writer.println(B2False);
            m_writer.println(id2 + " = 0");
            m_writer.println(S2Next);

            genCodeRelTestJump(parentMap.get("BTrue"), parentMap.get("BFalse"), id1 + " " + node.getValue() + " " + id2);
        }
        return null;
    }


    /*
    Même si on peut y avoir un grand nombre d'opération, celle-ci s'annullent entre elle.
    il est donc intéressant de vérifier si le nombre d'opération est pair ou impaire.
    Si le nombre d'opération est pair, on peut simplement ignorer ce noeud.
     */
    @Override
    public Object visit(ASTNotExpr node, Object data) {
        Map<String, String> parentMap = (HashMap<String, String>) data;

        int size = node.getOps().size();
        if (size % 2 == 0) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } else {
            Map<String, String> BchildMap =  new HashMap<>();
            BchildMap.put("BTrue", parentMap.get("BFalse"));
            BchildMap.put("BFalse", parentMap.get("BTrue"));
            BchildMap.put("SNext", parentMap.get("SNext"));
            return node.jjtGetChild(0).jjtAccept(this, BchildMap);

        }
    }

    @Override
    public Object visit(ASTGenValue node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    /*
    BoolValue ne peut pas simplement retourné sa valeur à son parent contrairement à GenValue et IntValue,
    Il doit plutôt généré des Goto direct, selon sa valeur.
     */
    @Override
    public Object visit(ASTBoolValue node, Object data) {
        Map<String, String> parentMap = (HashMap<String, String>) data;
        if (node.getValue())
            m_writer.println("goto " + parentMap.get("BTrue"));
        else
            m_writer.println("goto " + parentMap.get("BFalse"));
        return null;
    }


    /*
    si le type de la variable est booléenne, il faudra généré des goto ici.
    le truc est de faire un "if value == 1 goto Label".
    en effet, la structure "if valeurBool goto Label" n'existe pas dans la syntaxe du code à trois adresse.
     */
    @Override
    public Object visit(ASTIdentifier node, Object data) {
        Map<String, String> parentMap = (HashMap<String, String>) data;
        if(SymbolTable.get(node.getValue()) == VarType.Bool) {
            genCodeRelTestJump(parentMap.get("BTrue"), parentMap.get("BFalse"), node.getValue() + " == 1");
            return null;
        }
        Map<String, String> returnMap = new HashMap<>();
        returnMap.put("EAddr", node.getValue());
        return returnMap;
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        Map<String, String> returnMap = new HashMap<>();
        returnMap.put("EAddr", Integer.toString(node.getValue()));
        return returnMap;
    }

    //des outils pour vous simplifier la vie et vous enligner dans le travail
    public enum VarType {
        Bool,
        Number
    }

    //utile surtout pour envoyé de l'informations au enfant des expressions logiques.
    private class BoolLabel {
        public String lTrue = null;
        public String lFalse = null;
    }
}
