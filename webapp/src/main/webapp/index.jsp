<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html lang="es">
    <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <title>PAW</title>
        <link rel="stylesheet" href="<c:url value='/css/components.css' />" />
        <script defer src="<c:url value='/js/components.js' />"></script>
    </head>
    <body>

        <!-- Ejemplo del modal -->
        <paw:modal
            id="identificacion del modal"
            title="Titulo principal"
            message="Mensaje para mostrar"
            variant="default"
            size="sm"
            triggerLabel="Abrir modal"
            confirmLabel="Confirmar"
            cancelLabel="Cancelar">
            <p class="modal__supporting-text">
                Bloque de contenido extra
            </p>
        </paw:modal>
    </body>
</html>
