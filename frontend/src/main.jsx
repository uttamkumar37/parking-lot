import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { Toaster } from 'react-hot-toast'
import App from './App'
import './index.css'

class ErrorBoundary extends React.Component {
  constructor(props) { super(props); this.state = { error: null } }
  static getDerivedStateFromError(e) { return { error: e } }
  render() {
    if (this.state.error) {
      return (
        <div style={{padding:'2rem',fontFamily:'monospace',background:'#fee2e2',minHeight:'100vh'}}>
          <h1 style={{color:'#dc2626',fontSize:'1.25rem',marginBottom:'1rem'}}>React Error</h1>
          <pre style={{whiteSpace:'pre-wrap',fontSize:'0.875rem'}}>{this.state.error.toString()}</pre>
          <pre style={{whiteSpace:'pre-wrap',fontSize:'0.75rem',marginTop:'1rem',color:'#6b7280'}}>{this.state.error.stack}</pre>
        </div>
      )
    }
    return this.props.children
  }
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <ErrorBoundary>
    <BrowserRouter>
      <App />
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 4000,
          style: { borderRadius: '8px', fontSize: '14px' },
        }}
      />
    </BrowserRouter>
    </ErrorBoundary>
  </React.StrictMode>,
)
