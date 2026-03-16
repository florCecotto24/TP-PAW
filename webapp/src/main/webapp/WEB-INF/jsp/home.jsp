<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="paw" tagdir="/WEB-INF/tags" %>

<!DOCTYPE html>
<html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Spring Components Demo - Group 8</title>
        <link href="<c:url value="/css/components.css" />" rel="stylesheet" type="text/css">
        <style>
            * {
                box-sizing: border-box;
                margin: 0;
                padding: 0;
            }
            body {
                font-family: Arial, sans-serif;
                background-color: #f0f0f0;
                color: #333;
            }
            .container {
                max-width: 1200px;
                margin: 0 auto;
                padding: 1.5rem;
            }
            .content { margin-bottom: 2.5rem; }
            .example-title {
                font-size: 1.25rem;
                font-weight: bold;
                margin-bottom: 1rem;
                padding-bottom: 0.5rem;
                border-bottom: 2px solid #ccc;
            }
            .page-header { margin-bottom: 5rem; }
            .page-title { font-size: 2rem; font-weight: bold; margin: 0 0 0.25rem 0; }
            .page-subtitle { font-size: 1rem; color: #666; margin: 0 0 1rem 0; }
            .page-separator { border: 0; border-top: 2px solid #ccc; margin: 0 0 1.5rem 0; }
            .button-showcase { display: flex; flex-wrap: wrap; align-items: center; gap: 0.75rem; margin-bottom: 1rem; }
            .demo-row { display: flex; flex-wrap: nowrap; align-items: flex-start; gap: 1rem; margin-bottom: 1.5rem; justify-content: flex-start; }
            .demo-size-label { font-size: 0.9rem; color: #666; margin-bottom: 0.5rem; line-height: 1.2; min-height: 1.2em; flex-shrink: 0; }
            .carcard-col { display: flex; flex-direction: column; align-items: flex-start; }
            .carcard-wrap { display: inline-block; flex-shrink: 0; }
            .carcard-wrap.size-sm { transform: scale(0.6); transform-origin: center top; }
            .carcard-wrap.size-md { transform: scale(0.8); transform-origin: center top; }
            .carcard-wrap.size-lg { transform: scale(1); transform-origin: center top; }
            .modal-overlay--sm .modal__content { min-height: 220px; }
            .modal-overlay--md .modal__content { min-height: 340px; }
            .modal-overlay--lg .modal__content { min-height: 460px; }
            .modal__message { white-space: pre-line; }
        </style>
    </head>

    <body>
        <div class="container">
            <header class="page-header">
                <h1 class="page-title">Kartz</h1>
                <p class="page-subtitle">Spring Components Demo - Group 8</p>
                <hr class="page-separator">
            </header>

            <div class="content">
                <div class="example-title">Button & Modal</div>
                <div class="button-showcase">
                    <span data-modal-open="modal-sm"><paw:button text="Open modal SM" size="sm" type="primary" cssClass="btn-primary" /></span>
                    <paw:modal id="modal-sm" title="Small size modal" size="sm" message="Example content for small size modal."
                        confirmLabel="Accept" cancelLabel="Cancel" />
                    <span data-modal-open="modal-md"><paw:button text="Open modal MD" size="md" type="primary" cssClass="btn-primary" /></span>
                    <paw:modal id="modal-md" title="Medium size modal" size="md" message="Example content for medium size modal (default).
Lorem ipsum dolor sit amet consectetur adipiscing elit natoque purus feugiat, condimentum duis aenean vestibulum ut montes cras dui enim, curae conubia posuere in dictum convallis nisi nisl elementum. Ligula semper ac fusce vel faucibus placerat lacus, sagittis libero eros id nascetur porttitor quis, ornare hac torquent ad non parturient. Velit interdum luctus varius mi vel leo, nullam dictumst dui fringilla suscipit, gravida est donec massa vestibulum."
                        confirmLabel="Accept" cancelLabel="Cancel" />
                    <span data-modal-open="modal-lg"><paw:button text="Open modal LG" size="lg" type="primary" cssClass="btn-primary" /></span>
                    <paw:modal id="modal-lg" title="Large size modal" size="lg" message="Example content for large size modal.
Lorem ipsum dolor sit amet consectetur adipiscing elit natoque purus feugiat, condimentum duis aenean vestibulum ut montes cras dui enim, curae conubia posuere in dictum convallis nisi nisl elementum. Ligula semper ac fusce vel faucibus placerat lacus, sagittis libero eros id nascetur porttitor quis, ornare hac torquent ad non parturient. Velit interdum luctus varius mi vel leo, nullam dictumst dui fringilla suscipit, gravida est donec massa vestibulum.
Curabitur curae egestas tellus ac duis porta penatibus interdum mi, neque euismod volutpat sociosqu blandit dictum placerat vestibulum praesent ligula, maecenas malesuada nullam vivamus id mattis lacinia faucibus. Fermentum ligula nisl ornare lectus convallis rutrum sociosqu hac semper mi habitant iaculis et, dictum nostra primis rhoncus ad bibendum congue suspendisse potenti volutpat sodales enim. Rutrum venenatis ornare felis erat interdum scelerisque suspendisse elementum, convallis vestibulum montes cum ultrices iaculis."
                        confirmLabel="Accept" cancelLabel="Cancel" />
                </div>
            </div>

            <div class="content">
                <div class="example-title" >CarCard</div>
                <div class="demo-row">
                    <div class="carcard-col">
                        <div class="carcard-wrap size-sm">
                            <paw:carCard model="Corolla" brand="Toyota" stars="4" price="1200" image="https://www.buyatoyota.com/sharpr/bat/assets/img/vehicle-info/Corolla/2026/hero-image.png" reviews="128" />
                        </div>
                    </div>
                    <div class="carcard-col">
                        <div class="carcard-wrap size-md">
                            <paw:carCard model="Golf GTI" brand="Volkswagen" stars="5" price="1500" image="https://media.vw.mediaservice.avp.tech/media/fast/v3_02TXWhbdRjG__3VWbX5MCfdOdDGND1rzmZy0qanTWocUdoKbqyFTmvFSQknyWmSLl87PUndhLLhRcUPhsgGTqc3mwwXB3N4OejmhmzqvJL1YkJBvByoAz-ggqlXu3rfHzzPy3vxPK37YkfDFrw-2Xqg_P3oY3ObCPFGXYjOz8UOp2Fnax1CiFmeKFXMgjW8VLcKSrC5EjOb9eFCrbw4bMSNZGx7iyWK8Vgu1zS8oqttEZ1CdB1vj_VtEB5Pwy7XTdusDOWtbKPg25NNxlPZxEgqZ45kx8xEImWMWKnEM2NmPDeWMPLJ8TFzJGUYi1LbfVV0muWy8OaTldqiaYwu2rWqU8-Jrnxbc-zYqHhk-w3hHXTsUqFg2ZmKnSnWDbTrDF6hr4O-HwkME5gm-Cf9U_RvENpH6DqhTUK_ofoIH0ZbQzuD9jWRPUSeJzJL5Asi14i-RuwGQz0MjSPmcOt4l_DdRlrFfxD_Kj0vE6wQPI_aRXgTfYHYH4inEau4msifoAgUL30BQu-iDqEaRKvoS-gPiDm4BnCdw7OBdAh5GuUkyjX61ug_QaiPAZXoL0S30HX0Q-g3cX-J5yjeUygGygKBZ_F9h-9XfPeQu5FHkfcif4DcQhlFmUGpopymdwv1COplki8RXiB8lnCL8FXC36IdJLKXSIUoRGWiP6OfQL-A_gPiIt638F1G6sG_jv8vdp4ldBF1DfUO_uME6wRv0vsOvbfpvUNgN4FXeOoSwRW0XWjjRCLo_xB7ktjHiA_p3sL1Ka5vcP2L-z3c93D_jucjvG_jfR-fg-8zfBeQOpCeQ1pBOo3UQrqCdAv_Bv5b9Nynf52BA6gb7Hqc8DThu2i7iRxA9iN_j-Kmv0XoK0J3UYPEhvCcxHMJ6Sf8UXreZOcN5GUCy6J7cGJmcv_0_hf3zY08DMbDMNrO3ymr2iy1M1exqk56_tXM1NS8kZmslfPaSinvFNPtoMa1olUqFJ10KhnXzHK9aKYdu2FpObNi2WZ6Yn7WeCEzkTEy8bihmdV2nZxSrbqc1iq1vFW2rWZpuc3pmVlDK5RrWbNsmyuHraPL1pGGVc1ZaUOrWI6ZNx2zadn_Sw1_Z_vqf8gU4AfBAwAA.webp?width=864" reviews="89" />
                        </div>
                    </div>
                    <div class="carcard-col">
                        <div class="carcard-wrap size-lg">
                            <paw:carCard model="Serie 3" brand="BMW" stars="5" price="2200" image="https://www.bmw.com.mx/es/local/lista-de-precios-de-autos-bmw/_jcr_content/root/maincontent/contentblueprint_cop_245807772/contentblueprint_143/container/image.coreimg.png/1753708406694/bmw-3-series-ice-lci-modelfinder.png" reviews="56" />
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <script src="<c:url value="/js/components.js" />"></script>
    </body>