import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil
import javax.swing.*

/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

typeMapping = [
        (~/(?i)int/)                      : "Integer",
        (~/(?i)float|double|decimal|real/): "Double",
        (~/(?i)numeric|decimal/)          : "java.math.BigDecimal",
        (~/(?i)datetime|timestamp/)       : "java.util.Date",
        (~/(?i)date/)                     : "java.util.Date",
        (~/(?i)time/)                     : "java.util.Date",
        (~/(?i)boolean/)                  : "Boolean",
        (~/(?i)/)                         : "String"
]

def prompt = {
    JFrame jframe = new JFrame()
    String answer = JOptionPane.showInputDialog(jframe, it)
    jframe.dispose()
    answer
}

def interfaceSuffix = prompt("Enter Interface Suffix")

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable }.each { generate(it, dir, interfaceSuffix) }
}

def generate(table, dir, interfaceSuffix) {
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    new File(dir, className + interfaceSuffix + ".java").withPrintWriter { out -> generate(out, dir, table, className, interfaceSuffix, fields) }
}

def generate(out, dir, table, className, interfaceSuffix, fields) {
    def tableName = table.getName()
    def tableComment = table.getComment()

    def packageName = dir.toString().find(/\/src\/main\/java\/(.*)/).substring(15).replace("/",".")

    out.println "package $packageName;"
    out.println "import java.util.Date;"
    out.println ""
    out.println ""
    out.println "/** $tableComment */"
    out.println "public interface ${className}${interfaceSuffix} {"
    out.println ""

    fields.each() {
        if(it.comment != "") out.println "  /** ${it.comment} */"
        out.println "  ${it.type} get${it.name.capitalize()}();"
        out.println ""
    }

    out.println "}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           name : javaName(col.getName(), false),
                           column : col.getName(),
                           type : typeStr,
                           comment: col.getComment()]]
    }
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
