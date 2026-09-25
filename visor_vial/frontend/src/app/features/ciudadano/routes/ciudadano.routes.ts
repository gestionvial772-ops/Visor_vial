import { Routes } from "@angular/router";

export const CIUDADANO_ROUTES: Routes =[

    {
        path: '',
        redirectTo: 'nuevo-reporte',
        pathMatch: 'full'
    },

    {
        path: 'nuevo-reporte',

        loadComponent: ()=>
            import ('../nuevo-reporte/nuevo-reporte')
                .then(m => m.NuevoReporte)
    }
    

]
