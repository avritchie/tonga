cd "$(dirname $0)"
java --module-path "$(dirname $0)/lib/mac" --add-modules "javafx.base,javafx.graphics,javafx.swing" -jar "$(dirname $0)/Tonga.main"