<?php
/**
 * Static page content, copied from apps/web messages/es-AR.json and en.json
 * (namespaces: terminos, privacidad, dmca, contacto). Selected by LOCALE.
 */

declare(strict_types=1);

function page_terminos(): array
{
    if (LOCALE === 'en') {
        return [
            'meta_title' => 'Terms and Conditions — OpenMeme',
            'meta_description' => 'Read the Terms and Conditions for using OpenMeme, including user conduct, content ownership, liability, and governing law.',
            'title' => 'Terms and Conditions',
            'last_updated' => 'Last updated: January 1, 2025',
            'intro' => 'Welcome to OpenMeme. By accessing or using our website at openmeme.io, you agree to be bound by these Terms and Conditions. If you do not agree with any part of these terms, you must not use our services.',
            'sections' => [
                ['1. Acceptance of Terms', 'By creating an account, uploading content, or browsing the platform, you acknowledge that you have read, understood, and agree to be bound by these Terms. We reserve the right to update these terms at any time. Continued use of the platform after changes constitutes acceptance of the new terms.'],
                ['2. User Conduct', 'You agree to use OpenMeme only for lawful purposes and in a way that does not infringe the rights of others. Prohibited conduct includes: uploading illegal or offensive content, attempting to disrupt platform operations, impersonating others, collecting user data without consent, and engaging in any form of harassment or abuse.'],
                ['3. Content Ownership', 'Users retain ownership of the content they upload to OpenMeme. By uploading content, you grant OpenMeme a non-exclusive, royalty-free, worldwide license to display, distribute, and promote your content on the platform. You represent that you own or have the necessary rights to upload and share the content.'],
                ['4. Intellectual Property Rights', 'The OpenMeme name, logo, design, and platform features are owned by OpenMeme and protected by intellectual property laws. You may not copy, modify, distribute, or create derivative works based on our platform without explicit written permission.'],
                ['5. Disclaimer of Warranties', "OpenMeme is provided on an 'as is' and 'as available' basis. We make no warranties, expressed or implied, regarding the availability, reliability, or accuracy of the platform or its content. We do not guarantee that the platform will be uninterrupted or error-free."],
                ['6. Limitation of Liability', 'OpenMeme and its operators shall not be liable for any indirect, incidental, special, consequential, or punitive damages arising from your use of the platform. In no event shall our total liability exceed the amount paid by you, if any, for using OpenMeme.'],
                ['7. Governing Law', 'These Terms shall be governed by and construed in accordance with the laws. Any disputes arising from these terms shall be resolved in the competent courts.'],
                ['8. Changes to Terms', 'We reserve the right to modify these Terms at any time. Changes will be effective immediately upon posting. Your continued use of the platform after changes constitutes acceptance of the revised terms. We encourage you to review these terms periodically.'],
            ],
        ];
    }
    return [
        'meta_title' => 'Términos y Condiciones — OpenMeme',
        'meta_description' => 'Leé los Términos y Condiciones de uso de OpenMeme, incluyendo conducta del usuario, propiedad del contenido, responsabilidad y ley aplicable.',
        'title' => 'Términos y Condiciones',
        'last_updated' => 'Última actualización: 1 de enero de 2025',
        'intro' => 'Bienvenido a OpenMeme. Al acceder o utilizar nuestro sitio web en openmeme.io, aceptás estar sujeto a estos Términos y Condiciones. Si no estás de acuerdo con alguna parte de estos términos, no debés usar nuestros servicios.',
        'sections' => [
            ['1. Aceptación de los Términos', 'Al crear una cuenta, subir contenido o navegar la plataforma, reconocés que has leído, entendido y aceptado estos Términos. Nos reservamos el derecho de actualizar estos términos en cualquier momento. El uso continuado de la plataforma después de los cambios constituye la aceptación de los nuevos términos.'],
            ['2. Conducta del Usuario', 'Aceptás usar OpenMeme solo para fines lícitos y de manera que no infrinja los derechos de otros. La conducta prohibida incluye: subir contenido ilegal u ofensivo, intentar interrumpir las operaciones de la plataforma, hacerse pasar por otros, recolectar datos de usuarios sin consentimiento y participar en cualquier forma de acoso o abuso.'],
            ['3. Propiedad del Contenido', 'Los usuarios conservan la propiedad del contenido que suben a OpenMeme. Al subir contenido, otorgás a OpenMeme una licencia no exclusiva, libre de regalías y mundial para mostrar, distribuir y promocionar tu contenido en la plataforma. Declarás que poseés o tenés los derechos necesarios para subir y compartir el contenido.'],
            ['4. Derechos de Propiedad Intelectual', 'El nombre, logotipo, diseño y características de la plataforma OpenMeme son propiedad de OpenMeme y están protegidos por las leyes de propiedad intelectual. No podés copiar, modificar, distribuir o crear trabajos derivados basados en nuestra plataforma sin permiso explícito por escrito.'],
            ['5. Exención de Garantías', "OpenMeme se proporciona 'tal cual' y 'según disponibilidad'. No ofrecemos garantías, expresas o implícitas, sobre la disponibilidad, confiabilidad o precisión de la plataforma o su contenido. No garantizamos que la plataforma sea ininterrumpida o libre de errores."],
            ['6. Limitación de Responsabilidad', 'OpenMeme y sus operadores no serán responsables por daños indirectos, incidentales, especiales, consecuentes o punitivos que surjan del uso de la plataforma. En ningún caso nuestra responsabilidad total excederá el monto pagado por usted, si corresponde, por usar OpenMeme.'],
            ['7. Ley Aplicable', 'Estos Términos se regirán e interpretarán de acuerdo con las leyes. Cualquier disputa que surja de estos términos se resolverá en los tribunales competentes.'],
            ['8. Cambios a los Términos', 'Nos reservamos el derecho de modificar estos Términos en cualquier momento. Los cambios entrarán en vigor inmediatamente después de su publicación. Tu uso continuado de la plataforma después de los cambios constituye la aceptación de los términos revisados. Te recomendamos revisar estos términos periódicamente.'],
        ],
    ];
}

function page_privacidad(): array
{
    if (LOCALE === 'en') {
        return [
            'meta_title' => 'Privacy Policy — OpenMeme',
            'meta_description' => 'Read the Privacy Policy for OpenMeme, covering data collection, cookies, third-party services, and user rights.',
            'title' => 'Privacy Policy',
            'last_updated' => 'Last updated: January 1, 2025',
            'intro' => 'OpenMeme respects your privacy. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you visit our website. Please read this policy carefully.',
            'sections' => [
                ['1. Information We Collect', 'We may collect personal information such as your name and email address when you create an account or contact us. We also automatically collect certain information when you visit the platform, including your IP address, browser type, operating system, referring URLs, and usage patterns.'],
                ['2. How We Use Your Information', 'We use the information we collect to provide, maintain, and improve our services, to communicate with you, to personalize your experience, and to enforce our policies. We do not sell your personal information to third parties.'],
                ['3. Cookies', 'We use cookies and similar tracking technologies to enhance your browsing experience, analyze site traffic, and understand where our audience comes from. You can control cookie preferences through your browser settings. Disabling cookies may affect certain features of the platform.'],
                ['4. Third-Party Services', 'We may use third-party services such as analytics providers and hosting services that collect, monitor, and analyze data to improve our service. These third parties have their own privacy policies governing the use of your information.'],
                ['5. Your Rights', 'You have the right to access, update, or delete your personal information. You may also object to or restrict certain processing of your data. To exercise these rights, please contact us at support@openmeme.io.'],
                ['6. Data Security', 'We implement reasonable security measures to protect your personal information from unauthorized access, alteration, disclosure, or destruction. However, no method of transmission over the Internet is completely secure.'],
                ['7. Changes to This Policy', 'We may update this Privacy Policy from time to time. Changes will be posted on this page with an updated effective date. We encourage you to review this policy periodically.'],
            ],
        ];
    }
    return [
        'meta_title' => 'Política de Privacidad — OpenMeme',
        'meta_description' => 'Leé la Política de Privacidad de OpenMeme, incluyendo recopilación de datos, cookies, servicios de terceros y derechos del usuario.',
        'title' => 'Política de Privacidad',
        'last_updated' => 'Última actualización: 1 de enero de 2025',
        'intro' => 'OpenMeme respeta tu privacidad. Esta Política de Privacidad explica cómo recopilamos, usamos, divulgamos y protegemos tu información cuando visitás nuestro sitio web. Por favor, leé esta política cuidadosamente.',
        'sections' => [
            ['1. Información que Recopilamos', 'Podemos recopilar información personal como tu nombre y dirección de correo electrónico cuando creás una cuenta o nos contactás. También recopilamos automáticamente cierta información cuando visitás la plataforma, incluyendo tu dirección IP, tipo de navegador, sistema operativo, URLs de referencia y patrones de uso.'],
            ['2. Cómo Usamos tu Información', 'Usamos la información que recopilamos para proporcionar, mantener y mejorar nuestros servicios, comunicarnos con vos, personalizar tu experiencia y hacer cumplir nuestras políticas. No vendemos tu información personal a terceros.'],
            ['3. Cookies', 'Usamos cookies y tecnologías de rastreo similares para mejorar tu experiencia de navegación, analizar el tráfico del sitio y entender de dónde viene nuestra audiencia. Podés controlar las preferencias de cookies a través de la configuración de tu navegador. Deshabilitar las cookies puede afectar ciertas funciones de la plataforma.'],
            ['4. Servicios de Terceros', 'Podemos usar servicios de terceros como proveedores de análisis y servicios de alojamiento que recopilan, monitorean y analizan datos para mejorar nuestro servicio. Estos terceros tienen sus propias políticas de privacidad que rigen el uso de tu información.'],
            ['5. Tus Derechos', 'Tenés derecho a acceder, actualizar o eliminar tu información personal. También podés oponerte o restringir ciertos procesamientos de tus datos. Para ejercer estos derechos, contactanos a support@openmeme.io.'],
            ['6. Seguridad de los Datos', 'Implementamos medidas de seguridad razonables para proteger tu información personal contra acceso no autorizado, alteración, divulgación o destrucción. Sin embargo, ningún método de transmisión por Internet es completamente seguro.'],
            ['7. Cambios a esta Política', 'Podemos actualizar esta Política de Privacidad de vez en cuando. Los cambios se publicarán en esta página con una fecha de vigencia actualizada. Te recomendamos revisar esta política periódicamente.'],
        ],
    ];
}

function page_dmca(): array
{
    if (LOCALE === 'en') {
        return [
            'meta_title' => 'DMCA Policy — OpenMeme',
            'meta_description' => 'Read the DMCA Copyright Policy for OpenMeme, including how to report copyright infringement and file a counter-notice.',
            'title' => 'DMCA Policy',
            'last_updated' => 'Last updated: January 1, 2025',
            'intro' => 'OpenMeme respects the intellectual property rights of others. In accordance with the Digital Millennium Copyright Act (DMCA), we have adopted a policy to respond to notices of alleged copyright infringement.',
            'sections' => [
                ['1. Filing a DMCA Complaint', 'If you believe that your copyrighted work has been copied in a way that constitutes copyright infringement, please provide our Designated Copyright Agent with the following information: (a) your physical or electronic signature; (b) identification of the copyrighted work claimed to have been infringed; (c) identification of the material that is claimed to be infringing; (d) your contact information; (e) a statement that you have a good faith belief that the use is not authorized; and (f) a statement that the information in the notification is accurate.'],
                ['2. Counter-Notice Procedure', 'If you believe that material you posted was removed or disabled by mistake or misidentification, you may file a counter-notice. Your counter-notice must include: (a) your physical or electronic signature; (b) identification of the material that has been removed; (c) a statement under penalty of perjury that you have a good faith belief that the material was removed as a result of mistake or misidentification; and (d) your contact information.'],
                ['3. Repeat Infringers', 'OpenMeme reserves the right to terminate user accounts that are determined to be repeat infringers of copyright. We may also suspend or terminate accounts at our discretion based on the severity and frequency of infringing activity.'],
                ['4. Contact Information', 'All DMCA notices and counter-notices should be submitted to our Designated Copyright Agent at: support@openmeme.io. We aim to respond to all legitimate complaints within 5 business days.'],
            ],
        ];
    }
    return [
        'meta_title' => 'Política DMCA — OpenMeme',
        'meta_description' => 'Leé la Política de Derechos de Autor DMCA de OpenMeme, incluyendo cómo reportar infracciones de derechos de autor y presentar una contra-notificación.',
        'title' => 'Política DMCA',
        'last_updated' => 'Última actualización: 1 de enero de 2025',
        'intro' => 'OpenMeme respeta los derechos de propiedad intelectual de otros. De acuerdo con la Ley de Derechos de Autor del Milenio Digital (DMCA), hemos adoptado una política para responder a avisos de presunta infracción de derechos de autor.',
        'sections' => [
            ['1. Presentar una Queja DMCA', 'Si creés que tu trabajo protegido por derechos de autor ha sido copiado de una manera que constituye infracción, proporcioná a nuestro Agente de Derechos de Autor Designado la siguiente información: (a) tu firma física o electrónica; (b) identificación del trabajo protegido que se alega ha sido infringido; (c) identificación del material que se alega infringe; (d) tu información de contacto; (e) una declaración de buena fe; y (f) una declaración de que la información en la notificación es precisa.'],
            ['2. Procedimiento de Contra-Notificación', 'Si creés que el material que publicaste fue eliminado o deshabilitado por error o identificación errónea, podés presentar una contra-notificación. Debe incluir: (a) tu firma física o electrónica; (b) identificación del material eliminado; (c) una declaración bajo pena de perjurio de buena fe; y (d) tu información de contacto.'],
            ['3. Infractores Reincidentes', 'OpenMeme se reserva el derecho de cancelar las cuentas de usuarios que se determine que son infractores reincidentes de derechos de autor. También podemos suspender o cancelar cuentas a nuestra discreción según la gravedad y frecuencia de la actividad infractora.'],
            ['4. Información de Contacto', 'Todas las notificaciones DMCA y contra-notificaciones deben enviarse a nuestro Agente de Derechos de Autor Designado a: support@openmeme.io. Nos comprometemos a responder a todas las quejas legítimas dentro de 5 días hábiles.'],
        ],
    ];
}

function page_contacto(): array
{
    if (LOCALE === 'en') {
        return [
            'meta_title' => 'Contact Us — OpenMeme',
            'meta_description' => 'Get in touch with the OpenMeme team. Reach out to support@openmeme.io for questions, feedback, or support.',
            'title' => 'Contact Us',
            'eyebrow' => 'Get in touch',
            'description' => "Have a question, suggestion, or need help? We'd love to hear from you. Reach out to us via email and we'll get back to you as soon as possible.",
            'email_heading' => 'Email',
            'email_address' => 'support@openmeme.io',
            'response_time' => 'We aim to respond within 24-48 hours.',
        ];
    }
    return [
        'meta_title' => 'Contacto — OpenMeme',
        'meta_description' => 'Comunicate con el equipo de OpenMeme. Escribinos a support@openmeme.io para preguntas, comentarios o soporte.',
        'title' => 'Contacto',
        'eyebrow' => 'Comunicate con nosotros',
        'description' => '¿Tenés una pregunta, sugerencia o necesitás ayuda? Nos encantaría saber de vos. Contactanos por correo electrónico y te responderemos lo antes posible.',
        'email_heading' => 'Correo electrónico',
        'email_address' => 'support@openmeme.io',
        'response_time' => 'Respondemos dentro de 24-48 horas.',
    ];
}
